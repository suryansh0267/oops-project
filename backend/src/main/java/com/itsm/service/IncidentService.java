package com.itsm.service;

import com.itsm.dto.*;
import com.itsm.entity.*;
import com.itsm.enums.*;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventType;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.exception.WorkloadExceededException;
import com.itsm.repository.*;
import com.itsm.security.IncidentAccessPolicy;
import com.itsm.sla.SlaStatus;
import com.itsm.state.IncidentStateFactory;
import com.itsm.strategy.AssignmentStrategyManager;
import com.itsm.strategy.PriorityStrategyManager;
import com.itsm.intelligence.frustration.ProfanitySanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Incident lifecycle orchestration. Business rules live in collaborators:
 * state objects (legal transitions), strategy managers (priority and assignment),
 * IncidentAccessPolicy (who may do what) and event listeners (notifications).
 */
@Service
public class IncidentService {

    /** Used when no SLA policy is configured for a priority. */
    static final Duration DEFAULT_RESPONSE = Duration.ofHours(2);
    static final Duration DEFAULT_RESOLUTION = Duration.ofHours(24);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ITServiceRepository itServiceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final IncidentNoteRepository incidentNoteRepository;
    private final AuditService auditService;
    private final PriorityStrategyManager priorityStrategyManager;
    private final AssignmentStrategyManager assignmentStrategyManager;
    private final IncidentCategoryService categoryService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IncidentAccessPolicy accessPolicy;
    private final IncidentMapper mapper;
    private final ApplicationEventPublisher events;

    /** When true, new incidents are routed to a technician by the active strategy straight away. */
    @Value("${app.assignment.auto-on-create:true}")
    private boolean autoAssignOnCreate;

    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           ITServiceRepository itServiceRepository,
                           SLAPolicyRepository slaPolicyRepository,
                           TechnicianProfileRepository technicianProfileRepository,
                           IncidentNoteRepository incidentNoteRepository,
                           AuditService auditService,
                           PriorityStrategyManager priorityStrategyManager,
                           AssignmentStrategyManager assignmentStrategyManager,
                           IncidentCategoryService categoryService,
                           DuplicateDetectionService duplicateDetectionService,
                           IncidentAccessPolicy accessPolicy,
                           IncidentMapper mapper,
                           ApplicationEventPublisher events) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.itServiceRepository = itServiceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.incidentNoteRepository = incidentNoteRepository;
        this.auditService = auditService;
        this.priorityStrategyManager = priorityStrategyManager;
        this.assignmentStrategyManager = assignmentStrategyManager;
        this.categoryService = categoryService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.accessPolicy = accessPolicy;
        this.mapper = mapper;
        this.events = events;
    }

    // ================================================================== create

    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, String username) {
        User reporter = user(username);
        request.setTitle(ProfanitySanitizer.mask(request.getTitle()));
        request.setDescription(ProfanitySanitizer.mask(request.getDescription()));

        ITService service = null;
        int criticality = 2;
        if (request.getAffectedServiceId() != null) {
            service = itServiceRepository.findById(request.getAffectedServiceId())
                    .filter(ITService::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("IT service", "id", request.getAffectedServiceId()));
            criticality = service.getBusinessCriticality();
        }

        boolean autoCategorised = request.getCategory() == null || request.getCategory().isBlank();
        String category = autoCategorised
                ? categoryService.suggest(request.getTitle(), request.getDescription()).category()
                : categoryService.requireActive(request.getCategory());

        Impact impact = request.getImpact() != null ? request.getImpact() : Impact.MEDIUM;
        Urgency urgency = request.getUrgency() != null ? request.getUrgency() : Urgency.MEDIUM;
        Priority priority = priorityStrategyManager.calculate(impact, urgency, criticality, request.getAffectedUsers());
        Optional<SLAPolicy> sla = activePolicy(priority);

        Incident incident = Incident.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .reporter(reporter)
                .category(category)
                .impact(impact)
                .urgency(urgency)
                .priority(priority)
                .affectedUsers(request.getAffectedUsers())
                .status(IncidentStatus.CLASSIFIED)
                .affectedService(service)
                .slaPolicy(sla.orElse(null))
                .escalationLevel(0)
                .notes(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();
        applyDeadlines(incident, LocalDateTime.now(), sla);

        Incident saved = incidentRepository.save(incident);
        // Derive the number from the database id so it is unique across restarts.
        saved.setIncidentNumber(String.format("INC%05d", saved.getId()));
        saved = incidentRepository.save(saved);

        auditService.logAction(saved, reporter, "CREATED", null, IncidentStatus.NEW.name(), "Incident reported");
        auditService.logAction(saved, null, "CLASSIFIED", IncidentStatus.NEW.name(), IncidentStatus.CLASSIFIED.name(),
                (autoCategorised ? "Category suggested automatically: " : "Category: ") + category);
        auditService.logAction(saved, null, "PRIORITY_CALCULATED", null, priority.name(),
                "Priority " + priority + " via '" + priorityStrategyManager.getActiveStrategyKey()
                        + "' (impact " + impact + ", urgency " + urgency + ", criticality " + criticality
                        + (request.getAffectedUsers() != null ? ", " + request.getAffectedUsers() + " users" : "") + ")"
                        + (sla.isEmpty() ? ". No SLA policy for this priority, default targets applied." : ""));
        events.publishEvent(IncidentEvent.of(IncidentEventType.CREATED, saved, reporter));

        List<Incident> duplicates = duplicateDetectionService.findPotentialDuplicates(saved);
        if (!duplicates.isEmpty()) {
            String list = duplicates.stream().map(Incident::getIncidentNumber).reduce((a, b) -> a + ", " + b).orElse("");
            auditService.logAction(saved, null, "POSSIBLE_DUPLICATE", null, list, "Looks similar to " + list);
            events.publishEvent(new IncidentEvent(IncidentEventType.POSSIBLE_DUPLICATE, saved, reporter, list));
        }

        if (autoAssignOnCreate) {
            Optional<TechnicianProfile> technician = pickTechnician(saved, null);
            if (technician.isPresent()) {
                return assign(saved, technician.get().getUser().getId(), null, "AUTO_ASSIGNED");
            }
            auditService.logAction(saved, null, "QUEUED", null, null,
                    "No technician with free capacity; waiting for manual assignment");
        }
        return mapper.toResponse(saved);
    }

    // ================================================================== read

    public IncidentResponse getIncidentById(Long id, String username) {
        Incident incident = find(id);
        accessPolicy.assertCanView(user(username), incident);
        return mapper.toResponse(incident);
    }

    public List<IncidentResponse> getIncidentsByReporter(String username) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(user(username)).stream()
                .map(mapper::toResponse).toList();
    }

    public List<IncidentResponse> getIncidentsByTechnician(String username) {
        return incidentRepository.findByAssignedTechnicianOrderByCreatedAtDesc(user(username)).stream()
                .map(mapper::toResponse).toList();
    }

    /**
     * Search with up to nine criteria. Employees are always limited to their own incidents.
     * SLA status is derived, so it is filtered after the database query.
     */
    public List<IncidentResponse> search(String keyword, IncidentStatus status, Priority priority, String category,
                                         Long serviceId, Long technicianId, SlaStatus slaStatus,
                                         LocalDateTime from, LocalDateTime to, boolean mineOnly, String username) {
        return search(keyword, status, priority, category, serviceId, technicianId, slaStatus, from, to,
                mineOnly, username, null, false);
    }

    public List<IncidentResponse> search(String keyword, IncidentStatus status, Priority priority, String category,
                                         Long serviceId, Long technicianId, SlaStatus slaStatus,
                                         LocalDateTime from, LocalDateTime to, boolean mineOnly, String username,
                                         String frustration, boolean frustratedFirst) {
        User user = user(username);
        if (keyword != null && keyword.trim().toUpperCase(Locale.ROOT).matches("INC\\d+")) {
            return incidentRepository.findByIncidentNumberIgnoreCase(keyword.trim())
                    .filter(i -> accessPolicy.canView(user, i))
                    .map(mapper::toResponse).map(List::of).orElse(List.of());
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'From' date must be before 'to' date");
        }
        boolean restrictToReporter = mineOnly || user.getRole() == UserRole.EMPLOYEE;

        Specification<Incident> spec = (root, query, cb) -> cb.conjunction();
        for (Specification<Incident> part : Arrays.asList(
                IncidentSpecifications.keyword(keyword), IncidentSpecifications.status(status),
                IncidentSpecifications.priority(priority), IncidentSpecifications.category(category),
                IncidentSpecifications.service(serviceId), IncidentSpecifications.technician(technicianId),
                IncidentSpecifications.createdFrom(from), IncidentSpecifications.createdTo(to))) {
            if (part != null) spec = spec.and(part);
        }
        if (user.getRole() != UserRole.EMPLOYEE && frustration != null) {
            try { spec = spec.and(IncidentSpecifications.frustration(frustration)); }
            catch (IllegalArgumentException ex) { throw new BadRequestException("Unknown frustration filter"); }
        }
        if (restrictToReporter) {
            spec = spec.and(IncidentSpecifications.reporter(user));
        }

        LocalDateTime now = LocalDateTime.now();
        List<Incident> found = incidentRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (user.getRole() != UserRole.EMPLOYEE && frustratedFirst) {
            found.sort(Comparator.comparingInt((Incident i) -> i.getFrustrationLevel() == null ? -1
                    : i.getFrustrationLevel().ordinal()).reversed());
        }
        return found.stream()
                .filter(i -> slaStatus == null || mapper.slaStatus(i, now) == slaStatus)
                .map(i -> mapper.toResponse(i, now))
                .toList();
    }

    public List<NoteResponse> getNotes(Long id, String username) {
        User user = user(username);
        Incident incident = find(id);
        accessPolicy.assertCanView(user, incident);
        boolean internal = accessPolicy.seesInternalNotes(user);
        return incidentNoteRepository.findByIncidentOrderByCreatedAtAsc(incident).stream()
                .filter(n -> internal || !n.isInternalNote())
                .map(this::toNote)
                .toList();
    }

    public List<AuditLogResponse> getHistory(Long id, String username) {
        Incident incident = find(id);
        accessPolicy.assertCanView(user(username), incident);
        return auditService.getAuditLogs(id);
    }

    public List<SimilarIncident> getRelated(Long id, String username) {
        Incident incident = find(id);
        accessPolicy.assertCanView(user(username), incident);
        return duplicateDetectionService.findRelated(incident);
    }

    public List<ResolutionSuggestion> getSuggestedResolutions(Long id, String username) {
        Incident incident = find(id);
        accessPolicy.assertCanView(user(username), incident);
        return duplicateDetectionService.suggestResolutions(incident);
    }

    /** For staff-only views (recommendations) that need the entity itself. */
    public Incident findForStaff(Long id, String username) {
        Incident incident = find(id);
        accessPolicy.assertCanView(user(username), incident);
        return incident;
    }

    /** The lifecycle graph produced by the state objects (used by the UI and the report). */
    public Map<String, Object> lifecycle() {
        Map<String, Object> graph = new LinkedHashMap<>();
        IncidentStateFactory.all().forEach((status, state) -> graph.put(status.name(), Map.of(
                "label", state.label(),
                "open", state.isOpen(),
                "next", state.allowedTransitions().stream().map(Enum::name).sorted().toList())));
        return graph;
    }

    // ================================================================== update / triage

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request, String username) {
        Incident incident = find(id);
        User user = user(username);
        accessPolicy.assertCanView(user, incident);
        if (!IncidentStateFactory.of(incident.getStatus()).isOpen()) {
            throw new BadRequestException("Resolved or closed incidents cannot be edited");
        }
        boolean staff = user.getRole() != UserRole.EMPLOYEE;

        boolean priorityInputsChanged = false;
        if (staff && request.getImpact() != null && request.getImpact() != incident.getImpact()) {
            incident.setImpact(request.getImpact());
            priorityInputsChanged = true;
        }
        if (staff && request.getUrgency() != null && request.getUrgency() != incident.getUrgency()) {
            incident.setUrgency(request.getUrgency());
            priorityInputsChanged = true;
        }
        if (staff && request.getCategory() != null && !request.getCategory().isBlank()) {
            String newCategory = categoryService.requireActive(request.getCategory());
            if (!newCategory.equalsIgnoreCase(incident.getCategory())) {
                auditService.logAction(incident, user, "RECLASSIFIED", incident.getCategory(), newCategory,
                        "Category changed to " + newCategory);
                incident.setCategory(newCategory);
            }
        }

        if (priorityInputsChanged) {
            int criticality = incident.getAffectedService() != null ? incident.getAffectedService().getBusinessCriticality() : 2;
            Priority oldPriority = incident.getPriority();
            Priority newPriority = priorityStrategyManager.calculate(incident.getImpact(), incident.getUrgency(),
                    criticality, incident.getAffectedUsers());
            if (newPriority != oldPriority) {
                incident.setPriority(newPriority);
                Optional<SLAPolicy> sla = activePolicy(newPriority);
                incident.setSlaPolicy(sla.orElse(null));
                // Deadlines are measured from when the incident was reported.
                applyDeadlines(incident, incident.getCreatedAt() != null ? incident.getCreatedAt() : LocalDateTime.now(), sla);
                incident.setSlaWarningSentAt(null);
                auditService.logAction(incident, user, "PRIORITY_CHANGED", String.valueOf(oldPriority),
                        newPriority.name(), "Priority recalculated from " + oldPriority + " to " + newPriority
                                + "; SLA deadlines updated");
                events.publishEvent(new IncidentEvent(IncidentEventType.PRIORITY_CHANGED, incident, user,
                        "from " + oldPriority + " to " + newPriority));
            }
        }

        if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isBlank()) {
            saveNote(incident, user, ProfanitySanitizer.mask(request.getAdditionalInfo().trim()), false);
            auditService.logAction(incident, user, "INFO_ADDED", null, null, "Additional information added");
        }
        return mapper.toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse assignIncident(Long id, AssignRequest request, String username) {
        return assign(find(id), request.getTechnicianId(), user(username), "ASSIGNED");
    }

    @Transactional
    public IncidentResponse autoAssignIncident(Long id, String username) {
        Incident incident = find(id);
        User assigner = user(username);
        TechnicianProfile chosen = pickTechnician(incident, null)
                .orElseThrow(() -> new TechnicianUnavailableException("Every technician"));
        return assign(incident, chosen.getUser().getId(), assigner, "AUTO_ASSIGNED");
    }

    /**
     * Picks a technician with the active strategy, falling back to the least loaded one.
     * {@code exclude} lets escalation skip the current assignee.
     */
    public Optional<TechnicianProfile> pickTechnician(Incident incident, User exclude) {
        List<TechnicianProfile> available = technicianProfileRepository.findAvailableTechnicians().stream()
                .filter(p -> p.getUser() != null && p.getUser().isActive())
                .filter(p -> exclude == null || !Objects.equals(p.getUser().getId(), exclude.getId()))
                .toList();
        Optional<TechnicianProfile> chosen = assignmentStrategyManager.assign(incident, available);
        if (chosen.isEmpty()) {
            chosen = available.stream().min(Comparator.comparingInt(TechnicianProfile::getCurrentActiveIncidents));
        }
        return chosen;
    }

    /** Shared by manual assignment, auto-assignment and SLA escalation. */
    @Transactional
    public IncidentResponse assign(Incident incident, Long technicianId, User assigner, String auditAction) {
        if (assigner != null && assigner.getRole() == UserRole.EMPLOYEE) {
            throw new org.springframework.security.access.AccessDeniedException("Employees cannot assign incidents");
        }
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", technicianId));
        if (technician.getRole() != UserRole.TECHNICIAN && technician.getRole() != UserRole.ADMIN) {
            throw new BadRequestException(technician.getFullName() + " is not a technician");
        }
        User previous = incident.getAssignedTechnician();
        if (previous != null && Objects.equals(previous.getId(), technician.getId())) {
            throw new BadRequestException("Incident is already assigned to " + technician.getFullName());
        }

        TechnicianProfile profile = technicianProfileRepository.findByUser(technician).orElse(null);
        if (technician.getRole() == UserRole.TECHNICIAN) {
            if (profile == null || !profile.isAvailable() || !technician.isActive()) {
                throw new TechnicianUnavailableException(technician.getFullName());
            }
            if (profile.getCurrentActiveIncidents() >= profile.getMaxActiveIncidents()) {
                throw new WorkloadExceededException(technician.getFullName(), profile.getMaxActiveIncidents());
            }
        }

        IncidentStatus from = incident.getStatus();
        IncidentStateFactory.assertTransition(from, IncidentStatus.ASSIGNED);

        if (previous != null) {
            adjustWorkload(previous, -1, false);
        }
        if (profile != null) {
            adjustWorkload(technician, +1, false);
        }
        incident.setAssignedTechnician(technician);
        incident.setStatus(IncidentStatus.ASSIGNED);
        Incident saved = incidentRepository.save(incident);

        auditService.logAction(saved, assigner, auditAction,
                previous != null ? previous.getFullName() : "Unassigned", technician.getFullName(),
                (previous != null ? "Reassigned from " + previous.getFullName() + " to " : "Assigned to ")
                        + technician.getFullName()
                        + ("AUTO_ASSIGNED".equals(auditAction)
                        ? " by the '" + assignmentStrategyManager.getActiveStrategyKey() + "' strategy" : ""));
        events.publishEvent(IncidentEvent.of(IncidentEventType.ASSIGNED, saved, assigner));
        return mapper.toResponse(saved);
    }

    // ================================================================== technician work

    @Transactional
    public IncidentResponse acknowledgeIncident(Long id, String username) {
        Incident incident = find(id);
        User tech = user(username);
        accessPolicy.assertCanWork(tech, incident);
        transition(incident, IncidentStatus.ACKNOWLEDGED, tech, "ACKNOWLEDGED", "Technician acknowledged the incident");
        if (incident.getRespondedAt() == null) {
            incident.setRespondedAt(LocalDateTime.now()); // first response counts for the response SLA
        }
        Incident saved = incidentRepository.save(incident);
        events.publishEvent(IncidentEvent.of(IncidentEventType.ACKNOWLEDGED, saved, tech));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IncidentResponse startInvestigation(Long id, String username) {
        Incident incident = find(id);
        User tech = user(username);
        accessPolicy.assertCanWork(tech, incident);
        transition(incident, IncidentStatus.IN_PROGRESS, tech, "INVESTIGATION_STARTED", "Investigation started");
        return mapper.toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, ResolveRequest request, String username) {
        Incident incident = find(id);
        User tech = user(username);
        accessPolicy.assertCanWork(tech, incident);
        transition(incident, IncidentStatus.RESOLVED, tech, "RESOLVED", "Resolution: " + request.getResolutionNotes());

        LocalDateTime now = LocalDateTime.now();
        incident.setResolutionNotes(request.getResolutionNotes().trim());
        incident.setResolvedAt(now);
        if (incident.getAssignedTechnician() != null) {
            long minutes = incident.getCreatedAt() != null ? Duration.between(incident.getCreatedAt(), now).toMinutes() : 0;
            technicianProfileRepository.findByUser(incident.getAssignedTechnician()).ifPresent(p -> {
                int resolved = p.getTotalResolvedIncidents() + 1;
                p.setAvgResolutionTimeMinutes((p.getAvgResolutionTimeMinutes() * (resolved - 1) + minutes) / resolved);
                p.setTotalResolvedIncidents(resolved);
                p.setCurrentActiveIncidents(Math.max(0, p.getCurrentActiveIncidents() - 1));
                technicianProfileRepository.save(p);
            });
        }
        Incident saved = incidentRepository.save(incident);
        events.publishEvent(IncidentEvent.of(IncidentEventType.RESOLVED, saved, tech));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IncidentResponse requestInformation(Long id, String question, String username) {
        question = ProfanitySanitizer.mask(question);
        Incident incident = find(id);
        User tech = user(username);
        accessPolicy.assertCanWork(tech, incident);
        if (!IncidentStateFactory.of(incident.getStatus()).isOpen()) {
            throw new BadRequestException("Questions can only be asked while the incident is open");
        }
        saveNote(incident, tech, "[INFO_REQUEST] " + question.trim(), false);
        auditService.logAction(incident, tech, "INFO_REQUESTED", null, null, "Asked the reporter: " + question);
        events.publishEvent(new IncidentEvent(IncidentEventType.INFO_REQUESTED, incident, tech,
                tech.getFullName() + " asks: " + question));
        return mapper.toResponse(incident);
    }

    // ================================================================== reporter actions

    @Transactional
    public IncidentResponse respondToInformation(Long id, String response, String username) {
        response = ProfanitySanitizer.mask(response);
        Incident incident = find(id);
        User reporter = user(username);
        accessPolicy.assertIsReporterOrAdmin(reporter, incident);
        saveNote(incident, reporter, "[INFO_RESPONSE] " + response.trim(), false);
        auditService.logAction(incident, reporter, "INFO_PROVIDED", null, null, "Reporter replied: " + response);
        events.publishEvent(new IncidentEvent(IncidentEventType.INFO_PROVIDED, incident, reporter,
                reporter.getFullName() + " replied: " + response));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse verifyIncident(Long id, String username) {
        Incident incident = find(id);
        User user = user(username);
        accessPolicy.assertIsReporterOrAdmin(user, incident);
        transition(incident, IncidentStatus.VERIFIED, user, "VERIFIED", "Reporter confirmed the fix works");
        return mapper.toResponse(incidentRepository.save(incident));
    }

    /** Reporter says the fix did not work: back to the technician. */
    @Transactional
    public IncidentResponse reopenIncident(Long id, String reason, String username) {
        reason = ProfanitySanitizer.mask(reason);
        Incident incident = find(id);
        User user = user(username);
        accessPolicy.assertIsReporterOrAdmin(user, incident);
        transition(incident, IncidentStatus.IN_PROGRESS, user, "REOPENED", "Reopened: " + reason);
        incident.setResolvedAt(null);
        if (incident.getAssignedTechnician() != null) {
            adjustWorkload(incident.getAssignedTechnician(), +1, true);
        }
        saveNote(incident, user, "Reopened: " + reason.trim(), false);
        Incident saved = incidentRepository.save(incident);
        events.publishEvent(new IncidentEvent(IncidentEventType.REOPENED, saved, user, reason));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, String username) {
        Incident incident = find(id);
        User user = user(username);
        accessPolicy.assertCanClose(user, incident);
        transition(incident, IncidentStatus.CLOSED, user, "CLOSED", "Incident closed");
        incident.setClosedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);
        events.publishEvent(IncidentEvent.of(IncidentEventType.CLOSED, saved, user));
        return mapper.toResponse(saved);
    }

    @Transactional
    public IncidentResponse addFeedback(Long id, FeedbackRequest request, String username) {
        Incident incident = find(id);
        User user = user(username);
        accessPolicy.assertIsReporterOrAdmin(user, incident);
        if (IncidentStateFactory.of(incident.getStatus()).isOpen()) {
            throw new BadRequestException("Feedback can be given once the incident is resolved");
        }
        if (incident.getFeedbackRating() > 0) {
            throw new BadRequestException("Feedback has already been submitted for this incident");
        }
        incident.setFeedback(ProfanitySanitizer.mask(request.getFeedback()));
        incident.setFeedbackRating(request.getRating());
        auditService.logAction(incident, user, "FEEDBACK", null, request.getRating() + "/5", "Feedback submitted");
        return mapper.toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public NoteResponse addNote(Long id, NoteRequest request, String username) {
        Incident incident = find(id);
        User author = user(username);
        accessPolicy.assertCanView(author, incident);
        boolean internal = request.isInternalNote() && author.getRole() != UserRole.EMPLOYEE;
        IncidentNote note = saveNote(incident, author, ProfanitySanitizer.mask(request.getContent().trim()), internal);
        auditService.logAction(incident, author, internal ? "INTERNAL_NOTE" : "NOTE_ADDED", null, null,
                internal ? "Internal investigation note added" : "Note added");
        return toNote(note);
    }

    // ================================================================== helpers

    private void transition(Incident incident, IncidentStatus target, User actor, String action, String description) {
        IncidentStatus from = incident.getStatus();
        IncidentStateFactory.assertTransition(from, target);
        incident.setStatus(target);
        auditService.logAction(incident, actor, action, from != null ? from.name() : null, target.name(), description);
    }

    private void applyDeadlines(Incident incident, LocalDateTime start, Optional<SLAPolicy> sla) {
        incident.setResponseDeadline(start.plus(sla.map(p -> Duration.ofMinutes(p.getResponseTimeMinutes()))
                .orElse(DEFAULT_RESPONSE)));
        incident.setResolutionDeadline(start.plus(sla.map(p -> Duration.ofMinutes(p.getResolutionTimeMinutes()))
                .orElse(DEFAULT_RESOLUTION)));
    }

    private Optional<SLAPolicy> activePolicy(Priority priority) {
        return slaPolicyRepository.findByPriority(priority).filter(SLAPolicy::isActive);
    }

    private void adjustWorkload(User technician, int delta, boolean undoResolution) {
        technicianProfileRepository.findByUser(technician).ifPresent(p -> {
            p.setCurrentActiveIncidents(Math.max(0, p.getCurrentActiveIncidents() + delta));
            if (undoResolution) {
                p.setTotalResolvedIncidents(Math.max(0, p.getTotalResolvedIncidents() - 1));
            }
            technicianProfileRepository.save(p);
        });
    }

    private IncidentNote saveNote(Incident incident, User author, String content, boolean internal) {
        return incidentNoteRepository.save(IncidentNote.builder()
                .incident(incident)
                .author(author)
                .content(content)
                .isInternalNote(internal)
                .build());
    }

    private NoteResponse toNote(IncidentNote n) {
        String content = n.getContent();
        String kind = "NOTE";
        if (content.startsWith("[INFO_REQUEST] ") || content.startsWith("[SUPPORT REQUEST] ")) {
            kind = "INFO_REQUEST";
            content = content.substring(content.indexOf(']') + 2);
        } else if (content.startsWith("[INFO_RESPONSE] ") || content.startsWith("[CUSTOMER RESPONSE] ")) {
            kind = "INFO_RESPONSE";
            content = content.substring(content.indexOf(']') + 2);
        }
        return NoteResponse.builder()
                .id(n.getId())
                .content(content)
                .kind(kind)
                .internalNote(n.isInternalNote())
                .authorName(n.getAuthor() != null ? n.getAuthor().getFullName() : "System")
                .authorRole(n.getAuthor() != null ? n.getAuthor().getRole().name() : null)
                .createdAt(n.getCreatedAt())
                .build();
    }

    Incident find(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
