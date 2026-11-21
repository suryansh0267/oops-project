package com.itsm.controller;

import com.itsm.dto.ITServiceRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.dto.SLAPolicyRequest;
import com.itsm.dto.UserUpdateRequest;
import com.itsm.dto.admin.CategoryRequest;
import com.itsm.dto.admin.StrategyRequest;
import com.itsm.dto.admin.TechnicianProfileRequest;
import com.itsm.enums.UserRole;
import com.itsm.security.CustomUserDetails;
import com.itsm.service.AuditService;
import com.itsm.service.ITServiceService;
import com.itsm.service.IncidentCategoryService;
import com.itsm.service.NotificationService;
import com.itsm.service.SLAService;
import com.itsm.service.UserService;
import com.itsm.strategy.AssignmentStrategyManager;
import com.itsm.strategy.PriorityStrategyManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Administrator / IT manager API. Every route requires ROLE_ADMIN
 * (declared here and also enforced for /api/admin/** in SecurityConfig).
 * Dashboards and reports live in DashboardController.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ITServiceService itServiceService;
    private final SLAService slaService;
    private final AuditService auditService;
    private final IncidentCategoryService categoryService;
    private final AssignmentStrategyManager assignmentStrategyManager;
    private final PriorityStrategyManager priorityStrategyManager;
    private final NotificationService notificationService;

    // ==================== Users ====================

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
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        userService.deactivateUser(id, me.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    // ==================== Technicians ====================

    @GetMapping("/technicians")
    public ResponseEntity<?> getTechnicians() {
        return ResponseEntity.ok(userService.getTechnicianProfiles());
    }

    /** Creates the profile if it does not exist yet. */
    @PutMapping("/technicians/{userId}")
    public ResponseEntity<?> upsertTechnicianProfile(@PathVariable Long userId,
                                                     @Valid @RequestBody TechnicianProfileRequest request) {
        return ResponseEntity.ok(userService.upsertTechnicianProfile(userId, request));
    }

    // ==================== Incident categories ====================

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/categories/{id}/activate")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.setActive(id, true);
        return ResponseEntity.noContent().build();
    }

    // ==================== Strategies ====================

    @GetMapping("/assignment-strategies")
    public ResponseEntity<?> getAssignmentStrategies() {
        return ResponseEntity.ok(assignmentStrategyManager.getAvailableStrategies());
    }

    @PutMapping("/assignment-strategy")
    public ResponseEntity<?> setAssignmentStrategy(@Valid @RequestBody StrategyRequest body) {
        assignmentStrategyManager.setActiveStrategy(body.strategy());
        return ResponseEntity.ok(Map.of("activeStrategy", assignmentStrategyManager.getActiveStrategyKey()));
    }

    @GetMapping("/priority-strategies")
    public ResponseEntity<?> getPriorityStrategies() {
        return ResponseEntity.ok(priorityStrategyManager.getAvailableStrategies());
    }

    @PutMapping("/priority-strategy")
    public ResponseEntity<?> setPriorityStrategy(@Valid @RequestBody StrategyRequest body) {
        priorityStrategyManager.setActiveStrategy(body.strategy());
        return ResponseEntity.ok(Map.of("activeStrategy", priorityStrategyManager.getActiveStrategyKey()));
    }

    // ==================== IT services ====================

    @GetMapping("/services")
    public ResponseEntity<?> getAllServices() {
        return ResponseEntity.ok(itServiceService.getAllServices());
    }

    @PostMapping("/services")
    public ResponseEntity<?> createService(@Valid @RequestBody ITServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itServiceService.createService(request));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @Valid @RequestBody ITServiceRequest request) {
        return ResponseEntity.ok(itServiceService.updateService(id, request));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        itServiceService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/services/{id}/activate")
    public ResponseEntity<Void> activateService(@PathVariable Long id) {
        itServiceService.setActive(id, true);
        return ResponseEntity.noContent().build();
    }

    // ==================== SLA ====================

    @GetMapping("/sla-policies")
    public ResponseEntity<?> getAllPolicies() {
        return ResponseEntity.ok(slaService.getAllPolicies());
    }

    @PostMapping("/sla-policies")
    public ResponseEntity<?> createPolicy(@Valid @RequestBody SLAPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaService.createPolicy(request));
    }

    @PutMapping("/sla-policies/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id, @Valid @RequestBody SLAPolicyRequest request) {
        return ResponseEntity.ok(slaService.updatePolicy(id, request));
    }

    /** Open incidents currently past a deadline. */
    @GetMapping("/sla-violations")
    public ResponseEntity<?> currentViolations() {
        return ResponseEntity.ok(slaService.checkSLAViolations());
    }

    /** Open incidents past 75% of their window. */
    @GetMapping("/sla-approaching")
    public ResponseEntity<?> approaching() {
        return ResponseEntity.ok(slaService.getApproachingDeadlines());
    }

    /** Every breach the SLA monitor has recorded. */
    @GetMapping("/sla-violations/history")
    public ResponseEntity<?> violationHistory() {
        return ResponseEntity.ok(slaService.getRecordedViolations());
    }

    // ==================== Audit & notifications ====================

    /** Without a range: the latest 200 entries. */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> auditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (start == null && end == null) {
            return ResponseEntity.ok(auditService.getRecent());
        }
        LocalDateTime s = start != null ? start : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime e = end != null ? end : LocalDateTime.now();
        return ResponseEntity.ok(auditService.getAuditLogsByDateRange(s, e));
    }

    @GetMapping("/audit-logs/user/{username}")
    public ResponseEntity<?> auditLogsByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(username));
    }

    /** Simulated e-mail and SMS messages the system has sent. */
    @GetMapping("/notifications/outbox")
    public ResponseEntity<?> outbox() {
        return ResponseEntity.ok(notificationService.getOutbox());
    }
}
