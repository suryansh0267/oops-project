package com.itsm.intelligence.frustration;

import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventType;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.AuditService;
import com.itsm.service.IncidentService;
import com.itsm.dto.IncidentUpdateRequest;
import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import com.itsm.enums.UserRole;
import com.itsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.StringJoiner;

/** Pattern: Observer. Persists the raised flag and routes it through existing channels. */
@Slf4j @Component @RequiredArgsConstructor
public class FrustrationEventListener {
    private final FrustrationRecordRepository records;
    private final IncidentRepository incidents;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final UserRepository users;
    private final IncidentService incidentService;
    @Value("${app.frustration.auto-escalate:false}") private boolean autoEscalate;

    @EventListener
    public void onDetected(FrustrationDetectedEvent event) {
        if (event.incident() == null) return;
        try {
            String reasons = String.join("; ", event.assessment().reasons());
            FrustrationRecord record = new FrustrationRecord();
            record.setIncident(event.incident()); record.setUser(event.user());
            record.setLevel(event.assessment().level()); record.setScore(event.assessment().score());
            record.setReasons(reasons); record.setRecordedAt(clock.instant()); records.save(record);
            event.incident().setFrustrationLevel(event.assessment().level());
            event.incident().setFrustrationReasons(reasons);
            incidents.save(event.incident());
            audit.logAction(event.incident(), null, "FRUSTRATION_ALERT", null,
                    event.assessment().level().name(), "Heuristic frustration alert; staff review recommended");
            events.publishEvent(new IncidentEvent(IncidentEventType.FRUSTRATION_ALERT, event.incident(), event.user(),
                    event.assessment().level().name() + "; " + reasons));
            if (autoEscalate && event.assessment().level() == FrustrationLevel.CRITICAL) {
                users.findByRole(UserRole.ADMIN).stream().findFirst().ifPresent(admin -> {
                    try {
                        IncidentUpdateRequest update = new IncidentUpdateRequest();
                        update.setImpact(Impact.HIGH); update.setUrgency(Urgency.HIGH);
                        incidentService.updateIncident(event.incident().getId(), update, admin.getUsername());
                    } catch (RuntimeException ex) { log.warn("Optional priority escalation failed"); }
                });
            }
        } catch (RuntimeException ex) {
            log.warn("Frustration alert handling failed for incident {}", event.incident().getId());
        }
    }
}
