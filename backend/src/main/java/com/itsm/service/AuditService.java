package com.itsm.service;

import com.itsm.dto.AuditLogResponse;
import com.itsm.entity.AuditLog;
import com.itsm.entity.Incident;
import com.itsm.entity.User;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.AuditLogRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    /** {@code performedBy} may be null for automatic actions (SLA monitor, classifier). */
    @Transactional
    public void logAction(Incident incident, User performedBy, String action, String previousValue,
                          String newValue, String description) {
        auditLogRepository.save(AuditLog.builder()
                .incident(incident)
                .performedBy(performedBy)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description != null && description.length() > 250
                        ? description.substring(0, 247) + "..." : description)
                .build());
    }

    /** Oldest first, so it reads as a timeline. */
    public List<AuditLogResponse> getAuditLogs(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        return auditLogRepository.findByIncidentOrderByTimestampAsc(incident).stream().map(this::map).toList();
    }

    public List<AuditLogResponse> getAuditLogsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return auditLogRepository.findByPerformedByOrderByTimestampDesc(user).stream().map(this::map).toList();
    }

    public List<AuditLogResponse> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Start must be before end");
        }
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end).stream().map(this::map).toList();
    }

    public List<AuditLogResponse> getRecent() {
        return auditLogRepository.findTop200ByOrderByTimestampDesc().stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .previousValue(log.getPreviousValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .performedByName(log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : "System")
                .timestamp(log.getTimestamp())
                .incidentNumber(log.getIncident() != null ? log.getIncident().getIncidentNumber() : null)
                .incidentId(log.getIncident() != null ? log.getIncident().getId() : null)
                .build();
    }
}
