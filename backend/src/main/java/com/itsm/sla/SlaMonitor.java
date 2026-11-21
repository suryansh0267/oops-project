package com.itsm.sla;

import com.itsm.entity.Incident;
import com.itsm.entity.SlaViolation;
import com.itsm.entity.TechnicianProfile;
import com.itsm.enums.Priority;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventType;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.service.AuditService;
import com.itsm.service.IncidentService;
import com.itsm.state.IncidentStateFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Background SLA monitoring. Every minute it:
 * <ol>
 *   <li>warns the assignee once an open incident passes 75% of its resolution window;</li>
 *   <li>records response and resolution breaches (once each) and alerts admins;</li>
 *   <li>escalates unacknowledged P1/P2 incidents: level 1 reassigns to another technician,
 *       level 2 alerts the IT manager (admins) by SMS and e-mail.</li>
 * </ol>
 */
@Slf4j
@Component
public class SlaMonitor {

    private final IncidentRepository incidentRepository;
    private final SlaViolationRepository violationRepository;
    private final IncidentService incidentService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    public SlaMonitor(IncidentRepository incidentRepository, SlaViolationRepository violationRepository,
                      IncidentService incidentService, AuditService auditService,
                      ApplicationEventPublisher events, PlatformTransactionManager transactionManager) {
        this.incidentRepository = incidentRepository;
        this.violationRepository = violationRepository;
        this.incidentService = incidentService;
        this.auditService = auditService;
        this.events = events;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Value("${app.sla.escalation-enabled:true}")
    private boolean escalationEnabled;

    @Scheduled(fixedDelayString = "${app.sla.monitor-interval-ms:60000}", initialDelay = 30000)
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = incidentRepository.findByStatusIn(IncidentStateFactory.openStatuses())
                .stream().map(Incident::getId).toList();
        for (Long id : ids) {
            try {
                // One transaction per incident, so a failure on one never rolls back the others.
                tx.executeWithoutResult(status -> incidentRepository.findById(id).ifPresent(i -> check(i, now)));
            } catch (RuntimeException ex) {
                log.warn("SLA check failed for incident {}: {}", id, ex.getMessage());
            }
        }
        log.debug("SLA monitor checked {} open incidents", ids.size());
    }

    void check(Incident incident, LocalDateTime now) {
        boolean responded = incident.getRespondedAt() != null
                || IncidentStateFactory.of(incident.getStatus()).isResponded();

        // 1. Early warning
        if (incident.getSlaWarningSentAt() == null
                && SlaCalculator.elapsedFraction(incident.getCreatedAt(), incident.getResolutionDeadline(), now)
                >= SlaCalculator.AT_RISK_THRESHOLD
                && now.isBefore(incident.getResolutionDeadline())) {
            incident.setSlaWarningSentAt(now);
            long mins = SlaCalculator.minutesRemaining(incident.getResolutionDeadline(), now);
            auditService.logAction(incident, null, "SLA_WARNING", null, null,
                    "Resolution deadline in " + mins + " min");
            events.publishEvent(new IncidentEvent(IncidentEventType.SLA_WARNING, incident, null, mins + " minutes"));
        }

        // 2. Breaches
        if (!responded && SlaCalculator.isResponseBreached(incident.getResponseDeadline(), null, now)) {
            recordBreach(incident, SlaViolation.Type.RESPONSE, incident.getResponseDeadline());
        }
        if (incident.getResolutionDeadline() != null && now.isAfter(incident.getResolutionDeadline())) {
            recordBreach(incident, SlaViolation.Type.RESOLUTION, incident.getResolutionDeadline());
        }

        // 3. Escalation of unacknowledged high-priority incidents
        boolean high = incident.getPriority() == Priority.P1_CRITICAL || incident.getPriority() == Priority.P2_HIGH;
        if (escalationEnabled && high && !responded
                && incident.getResponseDeadline() != null && now.isAfter(incident.getResponseDeadline())) {
            escalate(incident, now);
        }
    }

    private void recordBreach(Incident incident, SlaViolation.Type type, LocalDateTime deadline) {
        if (violationRepository.existsByIncidentAndViolationType(incident, type)) {
            return;
        }
        violationRepository.save(SlaViolation.builder()
                .incident(incident)
                .violationType(type)
                .deadline(deadline)
                .technicianName(incident.getAssignedTechnician() != null
                        ? incident.getAssignedTechnician().getFullName() : null)
                .build());
        String what = type == SlaViolation.Type.RESPONSE ? "response" : "resolution";
        auditService.logAction(incident, null, "SLA_BREACHED", null, type.name(), "Missed the " + what + " target");
        events.publishEvent(new IncidentEvent(IncidentEventType.SLA_BREACHED, incident, null, what));
    }

    private void escalate(Incident incident, LocalDateTime now) {
        int level = incident.escalationLevelOrZero();
        if (level == 0) {
            Optional<TechnicianProfile> next = incidentService.pickTechnician(incident, incident.getAssignedTechnician());
            String from = incident.getAssignedTechnician() != null ? incident.getAssignedTechnician().getFullName() : "the queue";
            incident.setEscalationLevel(1);
            if (next.isPresent()) {
                incidentService.assign(incident, next.get().getUser().getId(), null, "ESCALATED");
                String msg = "Not acknowledged in time; moved from " + from + " to " + next.get().getUser().getFullName() + ".";
                auditService.logAction(incident, null, "ESCALATION_L1", "0", "1", msg);
                events.publishEvent(new IncidentEvent(IncidentEventType.ESCALATED, incident, null, msg));
                return;
            }
            level = 1; // nobody to reassign to: go straight to management
        }
        if (level == 1 && now.isAfter(incident.getResponseDeadline().plusMinutes(responseWindowMinutes(incident)))) {
            incident.setEscalationLevel(2);
            String msg = "Still not acknowledged after reassignment. IT manager action needed.";
            auditService.logAction(incident, null, "ESCALATION_L2", "1", "2", msg);
            events.publishEvent(new IncidentEvent(IncidentEventType.ESCALATED, incident, null, msg));
        }
    }

    /** The original response window length, reused as the grace period between escalation levels. */
    private static long responseWindowMinutes(Incident incident) {
        if (incident.getCreatedAt() == null || incident.getResponseDeadline() == null) {
            return 15;
        }
        return Math.max(5, java.time.Duration.between(incident.getCreatedAt(), incident.getResponseDeadline()).toMinutes());
    }
}
