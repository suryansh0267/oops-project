package com.itsm.controller;

import com.itsm.dto.*;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.intelligence.CategoryClassifier;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.DuplicateDetectionService;
import com.itsm.service.IncidentCategoryService;
import com.itsm.service.IncidentService;
import com.itsm.service.TechnicianRecommendationService;
import com.itsm.sla.SlaStatus;
import com.itsm.intelligence.frustration.FrustrationService;
import com.itsm.intelligence.frustration.ProfanitySanitizer;
import com.itsm.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Incident lifecycle API. Role checks are declared here with @PreAuthorize;
 * object-level checks (is this *your* incident?) happen in IncidentAccessPolicy.
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentCategoryService categoryService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final TechnicianRecommendationService recommendationService;
    private final FrustrationService frustrationService;
    private final UserRepository userRepository;

    // ------------------------------------------------------------ reporting

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(request, me.getUsername()));
    }

    /** Suggests a category while the reporter types (intelligent feature F). */
    @PostMapping("/classify")
    public CategoryClassifier.Suggestion classify(@Valid @RequestBody TextRequest draft) {
        return categoryService.suggest(draft.getTitle(), draft.getDescription());
    }

    /** Duplicate check before submitting (intelligent feature A). */
    @PostMapping("/similar")
    public List<SimilarIncident> similar(@Valid @RequestBody TextRequest draft) {
        return duplicateDetectionService.findSimilarToDraft(draft.getTitle(), draft.getDescription(),
                draft.getAffectedServiceId(), draft.getCategory());
    }

    // ------------------------------------------------------------ reading

    /**
     * Search and filter. Employees only ever get their own incidents.
     * Criteria: keyword / incident number, status, priority, category, service,
     * technician, SLA status, created-from and created-to.
     */
    @GetMapping
    public List<IncidentResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String frustration,
            @RequestParam(defaultValue = "false") boolean frustratedFirst,
            @AuthenticationPrincipal CustomUserDetails me) {
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTs = to != null ? to.plusDays(1).atStartOfDay().minusNanos(1) : null;
        return incidentService.search(q, status, priority, category, serviceId, technicianId, slaStatus,
                fromTs, toTs, mine, me.getUsername(), frustration, frustratedFirst);
    }

    @GetMapping("/my")
    public List<IncidentResponse> mine(@AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getIncidentsByReporter(me.getUsername());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public List<IncidentResponse> assigned(@AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getIncidentsByTechnician(me.getUsername());
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getIncidentById(id, me.getUsername());
    }

    @GetMapping("/{id}/notes")
    public List<NoteResponse> notes(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getNotes(id, me.getUsername());
    }

    /** Full audit trail, visible to anyone who can see the incident. */
    @GetMapping("/{id}/history")
    public List<AuditLogResponse> history(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getHistory(id, me.getUsername());
    }

    /** Correlated incidents: similar text or same service at the same time (feature B). */
    @GetMapping("/{id}/related")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public List<SimilarIncident> related(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getRelated(id, me.getUsername());
    }

    /** Fixes that worked for similar past incidents (feature G). */
    @GetMapping("/{id}/suggested-resolutions")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public List<ResolutionSuggestion> suggestions(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getSuggestedResolutions(id, me.getUsername());
    }

    /** Ranked technicians with score breakdown (feature D). */
    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TechnicianRecommendation> recommendations(@PathVariable Long id,
                                                          @AuthenticationPrincipal CustomUserDetails me) {
        return recommendationService.rank(incidentService.findForStaff(id, me.getUsername()));
    }

    // ------------------------------------------------------------ triage

    @PutMapping("/{id}")
    public IncidentResponse update(@PathVariable Long id, @Valid @RequestBody IncidentUpdateRequest request,
                                   @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.updateIncident(id, request, me.getUsername());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request,
                                   @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.assignIncident(id, request, me.getUsername());
    }

    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse autoAssign(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.autoAssignIncident(id, me.getUsername());
    }

    // ------------------------------------------------------------ technician work

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentResponse acknowledge(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.acknowledgeIncident(id, me.getUsername());
    }

    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentResponse investigate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.startInvestigation(id, me.getUsername());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request,
                                    @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.resolveIncident(id, request, me.getUsername());
    }

    @PostMapping("/{id}/request-info")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentResponse requestInfo(@PathVariable Long id, @Valid @RequestBody MessageRequest body,
                                        @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.requestInformation(id, body.getMessage(), me.getUsername());
    }

    // ------------------------------------------------------------ reporter actions

    @PostMapping("/{id}/respond-info")
    public IncidentResponse respondInfo(@PathVariable Long id, @Valid @RequestBody MessageRequest body,
                                        @AuthenticationPrincipal CustomUserDetails me) {
        IncidentResponse result = incidentService.respondToInformation(id, body.getMessage(), me.getUsername());
        detect(id, me, body.getMessage());
        return result;
    }

    @PostMapping("/{id}/verify")
    public IncidentResponse verify(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.verifyIncident(id, me.getUsername());
    }

    @PostMapping("/{id}/reopen")
    public IncidentResponse reopen(@PathVariable Long id, @Valid @RequestBody MessageRequest body,
                                   @AuthenticationPrincipal CustomUserDetails me) {
        IncidentResponse result = incidentService.reopenIncident(id, body.getMessage(), me.getUsername());
        detect(id, me, body.getMessage());
        return result;
    }

    @PostMapping("/{id}/close")
    public IncidentResponse close(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.closeIncident(id, me.getUsername());
    }

    @PostMapping("/{id}/feedback")
    public IncidentResponse feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request,
                                     @AuthenticationPrincipal CustomUserDetails me) {
        IncidentResponse result = incidentService.addFeedback(id, request, me.getUsername());
        detect(id, me, request.getFeedback());
        return result;
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<NoteResponse> addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request,
                                                @AuthenticationPrincipal CustomUserDetails me) {
        NoteResponse result = incidentService.addNote(id, request, me.getUsername());
        if (!request.isInternalNote()) detect(id, me, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/frustration/dismiss")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public Map<String, String> dismissFrustration(@PathVariable Long id,
                                                  @AuthenticationPrincipal CustomUserDetails me) {
        frustrationService.dismiss(id, userRepository.findByUsername(me.getUsername()).orElseThrow());
        return Map.of("message", "Flag dismissed until a new rise");
    }

    private void detect(Long id, CustomUserDetails me, String text) {
        try {
            var user = userRepository.findByUsername(me.getUsername()).orElseThrow();
            if (user.getRole() != com.itsm.enums.UserRole.EMPLOYEE) return;
            var incident = incidentService.findForStaff(id, me.getUsername());
            frustrationService.observe(user, incident, ProfanitySanitizer.mask(text), false);
        } catch (RuntimeException ignored) { /* monitoring cannot break primary workflows */ }
    }

    /** Lifecycle graph, so the UI and the report can show allowed transitions. */
    @GetMapping("/lifecycle")
    public Map<String, Object> lifecycle() {
        return incidentService.lifecycle();
    }
}
