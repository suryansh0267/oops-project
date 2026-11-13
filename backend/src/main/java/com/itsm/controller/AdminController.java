package com.itsm.controller;

import com.itsm.dto.ITServiceRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.dto.SLAPolicyRequest;
import com.itsm.enums.UserRole;
import com.itsm.service.AuditService;
import com.itsm.service.DashboardService;
import com.itsm.service.ITServiceService;
import com.itsm.service.SLAService;
import com.itsm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ITServiceService itServiceService;
    private final SLAService slaService;
    private final DashboardService dashboardService;
    private final AuditService auditService;
    private final com.itsm.service.IncidentCategoryService incidentCategoryService;
    private final com.itsm.strategy.AssignmentStrategyManager assignmentStrategyManager;

    // ==================== User Management ====================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/users/{userId}/technician-profile")
    public ResponseEntity<?> createTechnicianProfile(@PathVariable Long userId,
                                                     @RequestBody Map<String, Object> body) {
        List<String> skills = (List<String>) body.get("skills");
        String specialization = (String) body.get("specialization");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createTechnicianProfile(userId, skills, specialization));
    }

    @GetMapping("/technicians")
    public ResponseEntity<?> getTechnicians() {
        return ResponseEntity.ok(userService.getTechnicianProfiles());
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/technicians/{userId}")
    public ResponseEntity<?> updateTechnicianProfile(@PathVariable Long userId,
                                                     @RequestBody Map<String, Object> body) {
        List<String> skills = (List<String>) body.get("skills");
        String specialization = (String) body.get("specialization");
        Integer maxActive = body.get("maxActiveIncidents") != null ? ((Number) body.get("maxActiveIncidents")).intValue() : null;
        Boolean available = body.get("available") != null ? (Boolean) body.get("available") : null;
        return ResponseEntity.ok(userService.updateTechnicianProfile(userId, skills, maxActive, specialization, available));
    }

    // ==================== Incident Categories Configuration ====================

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(incidentCategoryService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentCategoryService.createCategory(name, description));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deactivateCategory(@PathVariable Long id) {
        incidentCategoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Assignment Strategy Configuration ====================

    @GetMapping("/assignment-strategies")
    public ResponseEntity<?> getAssignmentStrategies() {
        return ResponseEntity.ok(assignmentStrategyManager.getAvailableStrategies());
    }

    @PutMapping("/assignment-strategy")
    public ResponseEntity<?> setAssignmentStrategy(@RequestBody Map<String, String> body) {
        String strategy = body.get("strategy");
        assignmentStrategyManager.setActiveStrategy(strategy);
        return ResponseEntity.ok(Map.of(
                "message", "Active assignment strategy updated successfully",
                "activeStrategy", assignmentStrategyManager.getActiveStrategyKey()
        ));
    }

    // ==================== IT Service Management ====================

    @PostMapping("/services")
    public ResponseEntity<?> createService(@Valid @RequestBody ITServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itServiceService.createService(request));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id,
                                           @Valid @RequestBody ITServiceRequest request) {
        return ResponseEntity.ok(itServiceService.updateService(id, request));
    }

    @GetMapping("/services")
    public ResponseEntity<?> getAllServices() {
        return ResponseEntity.ok(itServiceService.getAllServices());
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<?> deactivateService(@PathVariable Long id) {
        itServiceService.deactivateService(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== SLA Policy Management ====================

    @PostMapping("/sla-policies")
    public ResponseEntity<?> createPolicy(@Valid @RequestBody SLAPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slaService.createPolicy(request));
    }

    @PutMapping("/sla-policies/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id,
                                          @Valid @RequestBody SLAPolicyRequest request) {
        return ResponseEntity.ok(slaService.updatePolicy(id, request));
    }

    @GetMapping("/sla-policies")
    public ResponseEntity<?> getAllPolicies() {
        return ResponseEntity.ok(slaService.getAllPolicies());
    }

    @GetMapping("/sla-violations")
    public ResponseEntity<?> checkSLAViolations() {
        return ResponseEntity.ok(slaService.checkSLAViolations());
    }

    @GetMapping("/sla-approaching")
    public ResponseEntity<?> getApproachingDeadlines() {
        return ResponseEntity.ok(slaService.getApproachingDeadlines());
    }

    // ==================== Audit Logs ====================

    @GetMapping("/audit-logs/incident/{incidentId}")
    public ResponseEntity<?> getAuditLogs(@PathVariable Long incidentId) {
        return ResponseEntity.ok(auditService.getAuditLogs(incidentId));
    }

    @GetMapping("/audit-logs/user/{username}")
    public ResponseEntity<?> getAuditLogsByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(username));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditService.getAuditLogsByDateRange(start, end));
    }
}
