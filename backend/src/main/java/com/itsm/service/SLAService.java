package com.itsm.service;

import com.itsm.dto.IncidentResponse;
import com.itsm.dto.SLAPolicyRequest;
import com.itsm.entity.Incident;
import com.itsm.entity.SLAPolicy;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SLAPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SLAService {

    private final SLAPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public SLAPolicy createPolicy(SLAPolicyRequest request) {
        if (slaPolicyRepository.findByPriority(request.getPriority()).isPresent()) {
            throw new DuplicateResourceException("SLA Policy already exists for priority: " + request.getPriority());
        }

        SLAPolicy policy = SLAPolicy.builder()
                .priority(request.getPriority())
                .responseTimeMinutes(request.getResponseTimeMinutes())
                .resolutionTimeMinutes(request.getResolutionTimeMinutes())
                .description(request.getDescription())
                .active(true)
                .build();

        return slaPolicyRepository.save(policy);
    }

    @Transactional
    public SLAPolicy updatePolicy(Long id, SLAPolicyRequest request) {
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLAPolicy", "id", id));

        policy.setPriority(request.getPriority());
        policy.setResponseTimeMinutes(request.getResponseTimeMinutes());
        policy.setResolutionTimeMinutes(request.getResolutionTimeMinutes());
        policy.setDescription(request.getDescription());

        return slaPolicyRepository.save(policy);
    }

    public List<SLAPolicy> getAllPolicies() {
        return slaPolicyRepository.findAll();
    }

    public List<IncidentResponse> checkSLAViolations() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> violations = incidentRepository.findSLAViolations(now);
        return violations.stream().map(this::mapToIncidentResponse).toList();
    }

    public List<IncidentResponse> getApproachingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(30);
        List<Incident> approaching = incidentRepository.findApproachingSLADeadlines(now, threshold);
        return approaching.stream().map(this::mapToIncidentResponse).toList();
    }

    public IncidentResponse mapToIncidentResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .incidentNumber(incident.getIncidentNumber())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .category(incident.getCategory() != null ? incident.getCategory().name() : null)
                .impact(incident.getImpact() != null ? incident.getImpact().name() : null)
                .urgency(incident.getUrgency() != null ? incident.getUrgency().name() : null)
                .priority(incident.getPriority() != null ? incident.getPriority().name() : null)
                .status(incident.getStatus() != null ? incident.getStatus().name() : null)
                .reporterName(incident.getReporter() != null ? incident.getReporter().getFullName() : null)
                .reporterId(incident.getReporter() != null ? incident.getReporter().getId() : null)
                .assignedTechnicianName(incident.getAssignedTechnician() != null ? incident.getAssignedTechnician().getFullName() : null)
                .assignedTechnicianId(incident.getAssignedTechnician() != null ? incident.getAssignedTechnician().getId() : null)
                .affectedServiceName(incident.getAffectedService() != null ? incident.getAffectedService().getName() : null)
                .affectedServiceId(incident.getAffectedService() != null ? incident.getAffectedService().getId() : null)
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .responseDeadline(incident.getResponseDeadline())
                .resolutionDeadline(incident.getResolutionDeadline())
                .respondedAt(incident.getRespondedAt())
                .resolvedAt(incident.getResolvedAt())
                .closedAt(incident.getClosedAt())
                .resolutionNotes(incident.getResolutionNotes())
                .feedback(incident.getFeedback())
                .feedbackRating(incident.getFeedbackRating())
                .build();
    }
}
