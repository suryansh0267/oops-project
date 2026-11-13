package com.itsm.service;

import com.itsm.dto.AuditLogResponse;
import com.itsm.entity.AuditLog;
import com.itsm.entity.Incident;
import com.itsm.entity.User;
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

    @Transactional
    public void logAction(Incident incident, User performedBy, String action, String previousValue, String newValue, String description) {
        AuditLog log = AuditLog.builder()
                .incident(incident)
                .performedBy(performedBy)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getAuditLogs(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        return auditLogRepository.findByIncidentOrderByTimestampDesc(incident).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return auditLogRepository.findByPerformedByOrderByTimestampDesc(user).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .previousValue(log.getPreviousValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .performedByName(log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : "System")
                .timestamp(log.getTimestamp())
                .incidentNumber(log.getIncident() != null ? log.getIncident().getIncidentNumber() : null)
                .build();
    }
}
