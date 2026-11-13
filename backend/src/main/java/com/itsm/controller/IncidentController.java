package com.itsm.controller;

import com.itsm.dto.AssignRequest;
import com.itsm.dto.FeedbackRequest;
import com.itsm.dto.IncidentRequest;
import com.itsm.dto.IncidentUpdateRequest;
import com.itsm.dto.NoteRequest;
import com.itsm.dto.ResolveRequest;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.DuplicateDetectionService;
import com.itsm.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final DuplicateDetectionService duplicateDetectionService;

    @PostMapping
    public ResponseEntity<?> createIncident(@Valid @RequestBody IncidentRequest request,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.createIncident(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIncident(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyIncidents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.getIncidentsByReporter(userDetails.getUsername()));
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedIncidents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.getIncidentsByTechnician(userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIncident(@PathVariable Long id,
                                            @Valid @RequestBody IncidentUpdateRequest request,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.updateIncident(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assignIncident(@PathVariable Long id,
                                            @Valid @RequestBody AssignRequest request,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.assignIncident(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<?> autoAssignIncident(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.autoAssignIncident(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeIncident(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.acknowledgeIncident(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/investigate")
    public ResponseEntity<?> startInvestigation(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.startInvestigation(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveIncident(@PathVariable Long id,
                                             @Valid @RequestBody ResolveRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.resolveIncident(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyIncident(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.verifyIncident(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeIncident(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.closeIncident(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id,
                                     @Valid @RequestBody NoteRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.addNote(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/request-info")
    public ResponseEntity<?> requestInfo(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, String> body,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        String question = body.getOrDefault("question", "Please provide additional details regarding this incident.");
        return ResponseEntity.ok(incidentService.requestInformation(id, question, userDetails.getUsername()));
    }

    @PostMapping("/{id}/respond-info")
    public ResponseEntity<?> respondInfo(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, String> body,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        String response = body.getOrDefault("response", "Additional details provided.");
        return ResponseEntity.ok(incidentService.respondToInformation(id, response, userDetails.getUsername()));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> addFeedback(@PathVariable Long id,
                                         @Valid @RequestBody FeedbackRequest request,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.addFeedback(id, request, userDetails.getUsername()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchIncidents(@RequestParam String keyword) {
        return ResponseEntity.ok(incidentService.searchIncidents(keyword));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> filterByStatus(@PathVariable IncidentStatus status) {
        return ResponseEntity.ok(incidentService.getIncidentsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<?> filterByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(incidentService.getIncidentsByPriority(priority));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> filterByCategory(@PathVariable IncidentCategory category) {
        return ResponseEntity.ok(incidentService.getIncidentsByCategory(category));
    }
}
