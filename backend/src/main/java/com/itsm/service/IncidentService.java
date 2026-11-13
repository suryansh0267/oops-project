package com.itsm.service;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.*;
import com.itsm.strategy.AssignmentStrategy;
import com.itsm.strategy.PriorityCalculationStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ITServiceRepository itServiceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final IncidentNoteRepository incidentNoteRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PriorityCalculationStrategy priorityCalculationStrategy;
    private final com.itsm.strategy.AssignmentStrategyManager assignmentStrategyManager;

    public IncidentService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            ITServiceRepository itServiceRepository,
            SLAPolicyRepository slaPolicyRepository,
            TechnicianProfileRepository technicianProfileRepository,
            IncidentNoteRepository incidentNoteRepository,
            NotificationService notificationService,
            AuditService auditService,
            @Qualifier("defaultPriority") PriorityCalculationStrategy priorityCalculationStrategy,
            com.itsm.strategy.AssignmentStrategyManager assignmentStrategyManager) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.itServiceRepository = itServiceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.incidentNoteRepository = incidentNoteRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.priorityCalculationStrategy = priorityCalculationStrategy;
        this.assignmentStrategyManager = assignmentStrategyManager;
    }

    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, String username) {
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ITService service = null;
        int criticality = 2; // default moderate
        if (request.getAffectedServiceId() != null) {
            service = itServiceRepository.findById(request.getAffectedServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("ITService", "id", request.getAffectedServiceId()));
            criticality = service.getBusinessCriticality();
        }

        Impact impact = request.getImpact() != null ? request.getImpact() : Impact.MEDIUM;
        Urgency urgency = request.getUrgency() != null ? request.getUrgency() : Urgency.MEDIUM;
        IncidentCategory category = request.getCategory() != null ? request.getCategory() : IncidentCategory.APPLICATION;

        Priority priority = priorityCalculationStrategy.calculatePriority(impact, urgency, criticality);
        SLAPolicy slaPolicy = slaPolicyRepository.findByPriority(priority).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline = slaPolicy != null ? now.plusMinutes(slaPolicy.getResponseTimeMinutes()) : now.plusHours(2);
        LocalDateTime resolutionDeadline = slaPolicy != null ? now.plusMinutes(slaPolicy.getResolutionTimeMinutes()) : now.plusHours(24);

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reporter(reporter)
                .category(category)
                .impact(impact)
                .urgency(urgency)
                .priority(priority)
                .status(IncidentStatus.CLASSIFIED)
                .affectedService(service)
                .slaPolicy(slaPolicy)
                .responseDeadline(responseDeadline)
                .resolutionDeadline(resolutionDeadline)
                .notes(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();

        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, reporter, "CREATED", null, "CLASSIFIED",
                "Incident created and auto-classified as " + category + " with priority " + priority);
        notificationService.notifyIncidentCreated(saved);

        return mapToResponse(saved);
    }

    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        return mapToResponse(incident);
    }

    public List<IncidentResponse> getIncidentsByReporter(String username) {
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return incidentRepository.findByReporter(reporter).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<IncidentResponse> getIncidentsByTechnician(String username) {
        User technician = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return incidentRepository.findByAssignedTechnician(technician).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        boolean priorityChanged = false;
        if (request.getImpact() != null && request.getImpact() != incident.getImpact()) {
            incident.setImpact(request.getImpact());
            priorityChanged = true;
        }
        if (request.getUrgency() != null && request.getUrgency() != incident.getUrgency()) {
            incident.setUrgency(request.getUrgency());
            priorityChanged = true;
        }
        if (request.getCategory() != null) {
            incident.setCategory(request.getCategory());
        }

        if (priorityChanged) {
            int criticality = incident.getAffectedService() != null ? incident.getAffectedService().getBusinessCriticality() : 2;
            Priority oldPriority = incident.getPriority();
            Priority newPriority = priorityCalculationStrategy.calculatePriority(incident.getImpact(), incident.getUrgency(), criticality);
            incident.setPriority(newPriority);

            SLAPolicy newSla = slaPolicyRepository.findByPriority(newPriority).orElse(null);
            incident.setSlaPolicy(newSla);

            auditService.logAction(incident, user, "PRIORITY_CHANGED",
                    oldPriority != null ? oldPriority.name() : null,
                    newPriority.name(), "Priority recalculated from " + oldPriority + " to " + newPriority);
        }

        if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isBlank()) {
            IncidentNote note = IncidentNote.builder()
                    .incident(incident)
                    .author(user)
                    .content(request.getAdditionalInfo())
                    .isInternalNote(false)
                    .build();
            incidentNoteRepository.save(note);
        }

        return mapToResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse assignIncident(Long id, AssignRequest request, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User assigner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        User technician = userRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", request.getTechnicianId()));

        if (technician.getRole() != UserRole.TECHNICIAN && technician.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Target user is not a technician or admin");
        }

        validateTransition(incident.getStatus(), IncidentStatus.ASSIGNED);

        User prevTech = incident.getAssignedTechnician();
        incident.setAssignedTechnician(technician);
        incident.setStatus(IncidentStatus.ASSIGNED);

        // Update workload counter
        technicianProfileRepository.findByUser(technician).ifPresent(profile -> {
            profile.setCurrentActiveIncidents(profile.getCurrentActiveIncidents() + 1);
            technicianProfileRepository.save(profile);
        });

        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, assigner, "ASSIGNED",
                prevTech != null ? prevTech.getFullName() : "Unassigned",
                technician.getFullName(), "Assigned to " + technician.getFullName());

        notificationService.notifyIncidentAssigned(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse autoAssignIncident(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        List<TechnicianProfile> availableTechs = technicianProfileRepository.findAvailableTechnicians();
        Optional<TechnicianProfile> recommended = assignmentStrategyManager.assign(incident, availableTechs);

        if (recommended.isEmpty()) {
            // Fallback: pick any available technician
            recommended = availableTechs.stream().min(Comparator.comparingInt(TechnicianProfile::getCurrentActiveIncidents));
        }

        if (recommended.isEmpty()) {
            throw new BadRequestException("No available technicians found for auto-assignment");
        }

        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId(recommended.get().getUser().getId());
        return assignIncident(id, assignRequest, username);
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User technician = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        validateTransition(incident.getStatus(), IncidentStatus.ACKNOWLEDGED);

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setRespondedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, technician, "ACKNOWLEDGED",
                IncidentStatus.ASSIGNED.name(), IncidentStatus.ACKNOWLEDGED.name(), "Technician acknowledged incident");

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse startInvestigation(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User technician = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        validateTransition(incident.getStatus(), IncidentStatus.IN_PROGRESS);

        incident.setStatus(IncidentStatus.IN_PROGRESS);
        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, technician, "INVESTIGATION_STARTED",
                IncidentStatus.ACKNOWLEDGED.name(), IncidentStatus.IN_PROGRESS.name(), "Technician started investigation");

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, ResolveRequest request, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User technician = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        validateTransition(incident.getStatus(), IncidentStatus.RESOLVED);

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolutionNotes(request.getResolutionNotes());
        incident.setResolvedAt(LocalDateTime.now());

        if (incident.getAssignedTechnician() != null) {
            technicianProfileRepository.findByUser(incident.getAssignedTechnician()).ifPresent(profile -> {
                profile.setCurrentActiveIncidents(Math.max(0, profile.getCurrentActiveIncidents() - 1));
                profile.setTotalResolvedIncidents(profile.getTotalResolvedIncidents() + 1);
                technicianProfileRepository.save(profile);
            });
        }

        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, technician, "RESOLVED",
                IncidentStatus.IN_PROGRESS.name(), IncidentStatus.RESOLVED.name(), "Incident resolved: " + request.getResolutionNotes());

        notificationService.notifyIncidentResolved(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse verifyIncident(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        validateTransition(incident.getStatus(), IncidentStatus.VERIFIED);

        incident.setStatus(IncidentStatus.VERIFIED);
        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, user, "VERIFIED",
                IncidentStatus.RESOLVED.name(), IncidentStatus.VERIFIED.name(), "Incident resolution verified");

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        validateTransition(incident.getStatus(), IncidentStatus.CLOSED);

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, user, "CLOSED",
                incident.getStatus() != null ? incident.getStatus().name() : null,
                IncidentStatus.CLOSED.name(), "Incident closed");

        return mapToResponse(saved);
    }

    @Transactional
    public Map<String, Object> addNote(Long id, NoteRequest request, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        IncidentNote note = IncidentNote.builder()
                .incident(incident)
                .author(author)
                .content(request.getContent())
                .isInternalNote(request.isInternalNote())
                .build();

        IncidentNote saved = incidentNoteRepository.save(note);

        return Map.of(
                "id", saved.getId(),
                "content", saved.getContent(),
                "authorName", author.getFullName(),
                "isInternalNote", saved.isInternalNote(),
                "createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now()
        );
    }

    @Transactional
    public IncidentResponse addFeedback(Long id, FeedbackRequest request, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        incident.setFeedback(request.getFeedback());
        incident.setFeedbackRating(request.getRating());

        return mapToResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse requestInformation(Long id, String question, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User technician = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        IncidentNote note = IncidentNote.builder()
                .incident(incident)
                .author(technician)
                .content("[SUPPORT REQUEST] " + question)
                .isInternalNote(false)
                .build();
        incidentNoteRepository.save(note);

        if (incident.getReporter() != null) {
            notificationService.sendNotification(
                    incident.getReporter(),
                    incident,
                    "Information Requested: " + incident.getIncidentNumber(),
                    "Support engineer " + technician.getFullName() + " requested details: " + question,
                    NotificationType.IN_APP
            );
        }

        auditService.logAction(incident, technician, "INFO_REQUESTED", null, null,
                "Technician requested additional details: " + question);

        return mapToResponse(incident);
    }

    @Transactional
    public IncidentResponse respondToInformation(Long id, String response, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        IncidentNote note = IncidentNote.builder()
                .incident(incident)
                .author(reporter)
                .content("[CUSTOMER RESPONSE] " + response)
                .isInternalNote(false)
                .build();
        incidentNoteRepository.save(note);

        if (incident.getAssignedTechnician() != null) {
            notificationService.sendNotification(
                    incident.getAssignedTechnician(),
                    incident,
                    "Response Received: " + incident.getIncidentNumber(),
                    "Reporter " + reporter.getFullName() + " provided additional details: " + response,
                    NotificationType.IN_APP
            );
        }

        auditService.logAction(incident, reporter, "INFO_PROVIDED", null, null,
                "Reporter responded to support request: " + response);

        return mapToResponse(incident);
    }

    public List<IncidentResponse> searchIncidents(String keyword) {
        return incidentRepository.searchByKeyword(keyword).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<IncidentResponse> getIncidentsByStatus(IncidentStatus status) {
        return incidentRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<IncidentResponse> getIncidentsByPriority(Priority priority) {
        return incidentRepository.findByPriority(priority).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<IncidentResponse> getIncidentsByCategory(IncidentCategory category) {
        return incidentRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateTransition(IncidentStatus current, IncidentStatus target) {
        if (current == null) return;

        boolean valid = switch (current) {
            case NEW -> target == IncidentStatus.CLASSIFIED;
            case CLASSIFIED -> target == IncidentStatus.ASSIGNED;
            case ASSIGNED -> target == IncidentStatus.ACKNOWLEDGED || target == IncidentStatus.ASSIGNED;
            case ACKNOWLEDGED -> target == IncidentStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == IncidentStatus.RESOLVED;
            case RESOLVED -> target == IncidentStatus.VERIFIED || target == IncidentStatus.CLOSED;
            case VERIFIED -> target == IncidentStatus.CLOSED;
            case CLOSED -> false;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(current.name(), target.name());
        }
    }

    public IncidentResponse mapToResponse(Incident incident) {
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
