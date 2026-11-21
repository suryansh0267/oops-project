package com.itsm.service;

import com.itsm.dto.NotificationResponse;
import com.itsm.entity.Incident;
import com.itsm.entity.Notification;
import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import com.itsm.enums.Priority;
import com.itsm.enums.UserRole;
import com.itsm.event.IncidentEvent;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.notification.NotificationChannel;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Decides who hears about each incident event and through which channels,
 * then fans the message out to every matching {@link NotificationChannel}.
 */
@Slf4j
@Service
public class NotificationService {

    private static final Set<NotificationType> APP = EnumSet.of(NotificationType.IN_APP);
    private static final Set<NotificationType> APP_EMAIL = EnumSet.of(NotificationType.IN_APP, NotificationType.EMAIL);
    private static final Set<NotificationType> URGENT = EnumSet.of(NotificationType.IN_APP, NotificationType.EMAIL, NotificationType.SMS);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<NotificationType, NotificationChannel> channels = new EnumMap<>(NotificationType.class);

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               List<NotificationChannel> channelList) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        channelList.forEach(c -> channels.put(c.type(), c));
        log.info("Notification channels available: {}", channels.keySet());
    }

    // ------------------------------------------------------------------ event routing

    @Transactional
    public void handle(IncidentEvent event) {
        Incident inc = event.incident();
        User actor = event.actor();
        User reporter = inc.getReporter();
        User tech = inc.getAssignedTechnician();
        String no = inc.getIncidentNumber();
        String title = "\"" + inc.getTitle() + "\"";
        boolean critical = inc.getPriority() == Priority.P1_CRITICAL;
        boolean high = critical || inc.getPriority() == Priority.P2_HIGH;
        String detail = event.detail() == null ? "" : event.detail();

        switch (event.type()) {
            case CREATED -> {
                send(reporter, null, inc, no + " logged",
                        "We received " + title + ". Priority " + label(inc.getPriority()) + ".", APP_EMAIL);
                sendToAdmins(null, inc, "New incident " + no,
                        title + " was reported (" + label(inc.getPriority()) + ").", critical ? URGENT : APP_EMAIL);
            }
            case POSSIBLE_DUPLICATE -> {
                send(reporter, actor, inc, no + " may already be known",
                        "Your report looks similar to " + detail + ". We will link them if so.", APP);
                sendToAdmins(actor, inc, "Possible duplicate: " + no, no + " looks similar to " + detail + ".", APP);
            }
            case ASSIGNED -> {
                send(tech, actor, inc, "Assigned to you: " + no,
                        title + " (" + label(inc.getPriority()) + ") is now in your queue.", critical ? URGENT : APP_EMAIL);
                send(reporter, null, inc, no + " assigned",
                        (tech != null ? tech.getFullName() : "A technician") + " is now handling " + title + ".", APP_EMAIL);
            }
            case PRIORITY_CHANGED -> {
                String msg = "Priority changed " + detail + ".";
                send(tech, actor, inc, no + " priority changed", msg, high ? APP_EMAIL : APP);
                send(reporter, actor, inc, no + " priority changed", msg, APP);
            }
            case ACKNOWLEDGED -> send(reporter, actor, inc, no + " acknowledged",
                    (tech != null ? tech.getFullName() : "A technician") + " has picked up " + title + ".", APP);
            case INFO_REQUESTED -> send(reporter, actor, inc, "Question about " + no, detail, APP_EMAIL);
            case INFO_PROVIDED -> send(tech, actor, inc, "Reply on " + no, detail, APP);
            case RESOLVED -> send(reporter, actor, inc, no + " resolved",
                    title + " has been resolved. Please confirm the fix or reopen it, and rate the support.", APP_EMAIL);
            case REOPENED -> {
                send(tech, actor, inc, no + " reopened", "The reporter says the issue is not fixed: " + detail, APP_EMAIL);
                sendToAdmins(actor, inc, no + " reopened", title + " was reopened by the reporter.", APP);
            }
            case CLOSED -> send(reporter, actor, inc, no + " closed", title + " is closed. Thank you.", APP);
            case SLA_WARNING -> {
                String msg = title + " will breach its SLA in " + detail + ".";
                if (tech != null) {
                    send(tech, null, inc, "SLA warning: " + no, msg, APP_EMAIL);
                } else {
                    sendToAdmins(null, inc, "SLA warning: " + no + " is unassigned", msg, APP_EMAIL);
                }
            }
            case SLA_BREACHED -> {
                sendToAdmins(null, inc, "SLA breached: " + no, title + " missed its " + detail + " target.", high ? URGENT : APP_EMAIL);
                send(tech, null, inc, "SLA breached: " + no, title + " missed its " + detail + " target.", APP);
            }
            case ESCALATED -> {
                send(tech, null, inc, "Escalated to you: " + no, detail, URGENT);
                sendToAdmins(null, inc, "Escalation: " + no, detail, URGENT);
            }
            case FRUSTRATION_ALERT -> {
                boolean isCritical = detail.startsWith("CRITICAL");
                String msg = "Heuristic frustration alert: " + detail + ". Review the conversation, contact the reporter and consider escalation.";
                if (tech != null) send(tech, null, inc, "Attention needed: " + no, msg, APP);
                if (isCritical || tech == null) sendToAdmins(null, inc, "Frustration escalation: " + no,
                        msg, isCritical ? URGENT : APP);
            }
            case FRUSTRATION_DISMISSED -> { /* audited separately; no outbound notification */ }
        }
    }

    private void sendToAdmins(User actor, Incident incident, String title, String message, Set<NotificationType> types) {
        for (User admin : userRepository.findByRole(UserRole.ADMIN)) {
            if (admin.isActive()) {
                send(admin, actor, incident, title, message, types);
            }
        }
    }

    /** Sends through each requested channel; skips the person who triggered the event. */
    private void send(User recipient, User actor, Incident incident, String title, String message,
                      Set<NotificationType> types) {
        if (recipient == null || (actor != null && Objects.equals(actor.getId(), recipient.getId()))) {
            return;
        }
        Set<NotificationType> wanted = new LinkedHashSet<>(types);
        wanted.add(NotificationType.IN_APP);
        for (NotificationType type : wanted) {
            NotificationChannel channel = channels.get(type);
            if (channel == null || !channel.canReach(recipient)) {
                continue;
            }
            try { channel.deliver(recipient, title, message); }
            catch (RuntimeException ex) { log.warn("{} channel failed for a notification", type); }
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .incident(incident)
                    .title(title)
                    .message(message)
                    .type(type)
                    .read(type != NotificationType.IN_APP)
                    .build());
        }
    }

    private static String label(Priority p) {
        if (p == null) return "unset";
        return p.name().replace('_', ' ');
    }

    // ------------------------------------------------------------------ queries

    public List<NotificationResponse> getNotifications(String username) {
        return notificationRepository
                .findTop50ByRecipientAndTypeOrderByCreatedAtDesc(user(username), NotificationType.IN_APP)
                .stream().map(this::mapToResponse).toList();
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientAndTypeAndReadFalse(user(username), NotificationType.IN_APP);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String username) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        if (!notification.getRecipient().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only update your own notifications");
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public Map<String, Object> markAllAsRead(String username) {
        List<Notification> unread = notificationRepository
                .findByRecipientAndTypeAndReadFalse(user(username), NotificationType.IN_APP);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
        return Map.of("count", unread.size());
    }

    /** Simulated e-mail and SMS messages, newest first, for the admin outbox. */
    public List<Map<String, Object>> getOutbox() {
        return notificationRepository
                .findTop100ByTypeInOrderByCreatedAtDesc(EnumSet.of(NotificationType.EMAIL, NotificationType.SMS))
                .stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "channel", n.getType().name(),
                        "to", n.getType() == NotificationType.SMS
                                ? String.valueOf(n.getRecipient().getPhoneNumber())
                                : String.valueOf(n.getRecipient().getEmail()),
                        "recipientName", String.valueOf(n.getRecipient().getFullName()),
                        "title", n.getTitle(),
                        "message", n.getMessage(),
                        "incidentNumber", n.getIncident() != null ? n.getIncident().getIncidentNumber() : "",
                        "createdAt", n.getCreatedAt()))
                .toList();
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .incidentNumber(n.getIncident() != null ? n.getIncident().getIncidentNumber() : null)
                .incidentId(n.getIncident() != null ? n.getIncident().getId() : null)
                .build();
    }
}
