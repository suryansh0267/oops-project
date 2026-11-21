package com.itsm.service;

import com.itsm.dto.IncidentResponse;
import com.itsm.dto.SLAPolicyRequest;
import com.itsm.entity.Incident;
import com.itsm.entity.SLAPolicy;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SLAPolicyRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.sla.SlaStatus;
import com.itsm.state.IncidentStateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SLAService {

    private final SLAPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final SlaViolationRepository slaViolationRepository;
    private final IncidentMapper mapper;

    @Transactional
    public SLAPolicy createPolicy(SLAPolicyRequest request) {
        validate(request);
        if (slaPolicyRepository.findByPriority(request.getPriority()).isPresent()) {
            throw new DuplicateResourceException("An SLA policy already exists for " + request.getPriority()
                    + ". Edit that policy instead.");
        }
        return slaPolicyRepository.save(SLAPolicy.builder()
                .priority(request.getPriority())
                .responseTimeMinutes(request.getResponseTimeMinutes())
                .resolutionTimeMinutes(request.getResolutionTimeMinutes())
                .description(request.getDescription())
                .active(true)
                .build());
    }

    @Transactional
    public SLAPolicy updatePolicy(Long id, SLAPolicyRequest request) {
        validate(request);
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy", "id", id));
        slaPolicyRepository.findByPriority(request.getPriority())
                .filter(other -> !Objects.equals(other.getId(), id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Another policy already covers " + request.getPriority());
                });
        policy.setPriority(request.getPriority());
        policy.setResponseTimeMinutes(request.getResponseTimeMinutes());
        policy.setResolutionTimeMinutes(request.getResolutionTimeMinutes());
        policy.setDescription(request.getDescription());
        return slaPolicyRepository.save(policy);
    }

    public List<SLAPolicy> getAllPolicies() {
        return slaPolicyRepository.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getPriority() == null ? 99 : p.getPriority().ordinal()))
                .toList();
    }

    /** Open incidents currently past a deadline. */
    public List<IncidentResponse> checkSLAViolations() {
        return openWithStatus(SlaStatus.BREACHED);
    }

    /** Open incidents more than 75% of the way to their resolution deadline (SLA risk detection). */
    public List<IncidentResponse> getApproachingDeadlines() {
        return openWithStatus(SlaStatus.AT_RISK);
    }

    /** Permanent breach records written by the SLA monitor. */
    public List<Map<String, Object>> getRecordedViolations() {
        return slaViolationRepository.findTop200ByOrderByDetectedAtDesc().stream()
                .map(v -> Map.<String, Object>of(
                        "id", v.getId(),
                        "incidentId", v.getIncident().getId(),
                        "incidentNumber", String.valueOf(v.getIncident().getIncidentNumber()),
                        "title", String.valueOf(v.getIncident().getTitle()),
                        "priority", String.valueOf(v.getIncident().getPriority()),
                        "type", v.getViolationType().name(),
                        "deadline", v.getDeadline() == null ? "" : v.getDeadline().toString(),
                        "detectedAt", v.getDetectedAt().toString(),
                        "technician", v.getTechnicianName() == null ? "Unassigned" : v.getTechnicianName()))
                .toList();
    }

    private List<IncidentResponse> openWithStatus(SlaStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return incidentRepository.findByStatusIn(IncidentStateFactory.openStatuses()).stream()
                .filter(i -> mapper.slaStatus(i, now) == status)
                .sorted(Comparator.comparing(Incident::getResolutionDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> mapper.toResponse(i, now))
                .toList();
    }

    private static void validate(SLAPolicyRequest r) {
        if (r.getPriority() == null) {
            throw new BadRequestException("Priority is required");
        }
        if (r.getResolutionTimeMinutes() < r.getResponseTimeMinutes()) {
            throw new BadRequestException("Resolution target must be at least as long as the response target");
        }
    }
}
