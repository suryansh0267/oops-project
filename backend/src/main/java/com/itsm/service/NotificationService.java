package com.itsm.service;

import com.itsm.dto.NotificationResponse;
import com.itsm.entity.Incident;
import com.itsm.entity.Notification;
import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import com.itsm.enums.UserRole;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification sendNotification(User recipient, Incident incident, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .incident(incident)
                .title(title)
                .message(message)
                .type(type != null ? type : NotificationType.IN_APP)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Simulate multi-channel notifications (Observer pattern / simulated dispatch)
        log.info("[{}] Notification to {} ({}): {} - {}",
                saved.getType(), recipient.getUsername(), recipient.getEmail(), title, message);

        return saved;
    }

    public void notifyIncidentCreated(Incident incident) {
        if (incident.getReporter() != null) {
            sendNotification(incident.getReporter(), incident,
                    "Incident Reported: " + incident.getIncidentNumber(),
                    "Your incident '" + incident.getTitle() + "' has been reported successfully with priority " + incident.getPriority(),
                    NotificationType.IN_APP);
        }
        // Also notify admins
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        for (User admin : admins) {
            sendNotification(admin, incident,
                    "New Incident: " + incident.getIncidentNumber(),
                    "A new incident has been reported: " + incident.getTitle(),
                    NotificationType.IN_APP);
        }
    }

    public void notifyIncidentAssigned(Incident incident) {
        if (incident.getAssignedTechnician() != null) {
            sendNotification(incident.getAssignedTechnician(), incident,
                    "Incident Assigned: " + incident.getIncidentNumber(),
                    "You have been assigned incident '" + incident.getTitle() + "' (Priority: " + incident.getPriority() + ")",
                    NotificationType.IN_APP);
        }
    }

    public void notifyIncidentResolved(Incident incident) {
        if (incident.getReporter() != null) {
            sendNotification(incident.getReporter(), incident,
                    "Incident Resolved: " + incident.getIncidentNumber(),
                    "Your incident '" + incident.getTitle() + "' has been resolved. Please review and provide feedback.",
                    NotificationType.IN_APP);
        }
    }

    public void notifySLAApproaching(Incident incident) {
        if (incident.getAssignedTechnician() != null) {
            sendNotification(incident.getAssignedTechnician(), incident,
                    "SLA Warning: " + incident.getIncidentNumber(),
                    "Incident '" + incident.getTitle() + "' SLA deadline is approaching in 30 minutes!",
                    NotificationType.IN_APP);
        }
    }

    public void notifySLAViolation(Incident incident) {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        for (User admin : admins) {
            sendNotification(admin, incident,
                    "SLA Breach: " + incident.getIncidentNumber(),
                    "Incident '" + incident.getTitle() + "' has breached its SLA deadline!",
                    NotificationType.IN_APP);
        }
    }

    public List<NotificationResponse> getNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public Map<String, Object> markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Notification> unread = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(user);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setRead(true);
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return Map.of("message", "All notifications marked as read", "count", unread.size());
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
                .build();
    }
}
