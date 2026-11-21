package com.itsm.controller;

import com.itsm.dto.IncidentResponse;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.IncidentService;
import com.itsm.service.UserService;
import com.itsm.dto.admin.TechnicianProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Technician self-service. Lifecycle actions (acknowledge, resolve...) live on
 * /api/incidents so there is exactly one route per action.
 */
@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
public class TechnicianController {

    private final IncidentService incidentService;
    private final UserService userService;

    @GetMapping("/incidents")
    public List<IncidentResponse> myQueue(@AuthenticationPrincipal CustomUserDetails me) {
        return incidentService.getIncidentsByTechnician(me.getUsername());
    }

    /** "View current workload": capacity, active count, resolved count, average time. */
    @GetMapping("/workload")
    public Map<String, Object> myWorkload(@AuthenticationPrincipal CustomUserDetails me) {
        return userService.getMyProfile(me.getUsername());
    }

    /** Technicians can mark themselves available/unavailable (e.g. on leave). */
    @PutMapping("/availability")
    public Map<String, Object> setAvailability(@RequestBody Map<String, Boolean> body,
                                               @AuthenticationPrincipal CustomUserDetails me) {
        Long userId = me.getUser().getId();
        return userService.upsertTechnicianProfile(userId,
                new TechnicianProfileRequest(null, null, null, Boolean.TRUE.equals(body.get("available"))));
    }
}
