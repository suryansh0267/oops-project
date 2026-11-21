package com.itsm.service;

import com.itsm.dto.IncidentResponse;
import com.itsm.entity.Incident;
import com.itsm.sla.SlaCalculator;
import com.itsm.sla.SlaStatus;
import com.itsm.state.IncidentState;
import com.itsm.state.IncidentStateFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Single place that turns an Incident entity into its API representation. */
@Component
public class IncidentMapper {

    public IncidentResponse toResponse(Incident incident) {
        return toResponse(incident, LocalDateTime.now());
    }

    public IncidentResponse toResponse(Incident i, LocalDateTime now) {
        IncidentState state = IncidentStateFactory.of(i.getStatus());
        return IncidentResponse.builder()
                .id(i.getId())
                .incidentNumber(i.getIncidentNumber())
                .title(i.getTitle())
                .description(i.getDescription())
                .category(i.getCategory())
                .impact(i.getImpact() != null ? i.getImpact().name() : null)
                .urgency(i.getUrgency() != null ? i.getUrgency().name() : null)
                .priority(i.getPriority() != null ? i.getPriority().name() : null)
                .status(state.status().name())
                .statusLabel(state.label())
                .allowedNextStatuses(state.allowedTransitions().stream().map(Enum::name).sorted().toList())
                .reporterName(i.getReporter() != null ? i.getReporter().getFullName() : null)
                .reporterId(i.getReporter() != null ? i.getReporter().getId() : null)
                .assignedTechnicianName(i.getAssignedTechnician() != null ? i.getAssignedTechnician().getFullName() : null)
                .assignedTechnicianId(i.getAssignedTechnician() != null ? i.getAssignedTechnician().getId() : null)
                .affectedServiceName(i.getAffectedService() != null ? i.getAffectedService().getName() : null)
                .affectedServiceId(i.getAffectedService() != null ? i.getAffectedService().getId() : null)
                .affectedUsers(i.getAffectedUsers())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .responseDeadline(i.getResponseDeadline())
                .resolutionDeadline(i.getResolutionDeadline())
                .respondedAt(i.getRespondedAt())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .slaStatus(slaStatus(i, now).name())
                .slaElapsedPercent((int) Math.min(999, Math.round(
                        SlaCalculator.elapsedFraction(i.getCreatedAt(), i.getResolutionDeadline(),
                                i.getResolvedAt() != null ? i.getResolvedAt() : now) * 100)))
                .escalationLevel(i.escalationLevelOrZero())
                .resolutionNotes(i.getResolutionNotes())
                .feedback(i.getFeedback())
                .feedbackRating(i.getFeedbackRating())
                .build();
    }

    public SlaStatus slaStatus(Incident i, LocalDateTime now) {
        return SlaCalculator.status(i.getCreatedAt(), i.getResponseDeadline(), i.getResolutionDeadline(),
                i.getRespondedAt(), i.getResolvedAt(), IncidentStateFactory.of(i.getStatus()).isOpen(), now);
    }
}
