package com.itsm.controller;

import com.itsm.dto.NoteRequest;
import com.itsm.dto.ResolveRequest;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
public class TechnicianController {

    private final IncidentService incidentService;

    @GetMapping("/incidents")
    public ResponseEntity<?> getAssignedIncidents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.getIncidentsByTechnician(userDetails.getUsername()));
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeIncident(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.acknowledgeIncident(id, userDetails.getUsername()));
    }

    @PostMapping("/incidents/{id}/investigate")
    public ResponseEntity<?> startInvestigation(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.startInvestigation(id, userDetails.getUsername()));
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<?> resolveIncident(@PathVariable Long id,
                                             @Valid @RequestBody ResolveRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(incidentService.resolveIncident(id, request, userDetails.getUsername()));
    }

    @PostMapping("/incidents/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id,
                                     @Valid @RequestBody NoteRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.addNote(id, request, userDetails.getUsername()));
    }
}
