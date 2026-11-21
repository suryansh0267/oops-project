package com.itsm.security;

import com.itsm.entity.Incident;
import com.itsm.entity.User;
import com.itsm.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class IncidentAccessPolicyTests {

    private final IncidentAccessPolicy policy = new IncidentAccessPolicy();
    private final User reporter = User.builder().id(1L).role(UserRole.EMPLOYEE).build();
    private final User otherEmployee = User.builder().id(2L).role(UserRole.EMPLOYEE).build();
    private final User assignee = User.builder().id(3L).role(UserRole.TECHNICIAN).build();
    private final User otherTech = User.builder().id(4L).role(UserRole.TECHNICIAN).build();
    private final User admin = User.builder().id(5L).role(UserRole.ADMIN).build();
    private final Incident incident = Incident.builder().id(10L).reporter(reporter).assignedTechnician(assignee).build();

    @Test
    @DisplayName("Employees only see their own incidents; staff see all")
    void viewRules() {
        assertTrue(policy.canView(reporter, incident));
        assertFalse(policy.canView(otherEmployee, incident));
        assertTrue(policy.canView(otherTech, incident));
        assertTrue(policy.canView(admin, incident));
    }

    @Test
    @DisplayName("Only the assignee or an admin can work the incident")
    void workRules() {
        assertDoesNotThrow(() -> policy.assertCanWork(assignee, incident));
        assertDoesNotThrow(() -> policy.assertCanWork(admin, incident));
        assertThrows(AccessDeniedException.class, () -> policy.assertCanWork(otherTech, incident));
        assertThrows(AccessDeniedException.class, () -> policy.assertCanWork(reporter, incident));
    }

    @Test
    @DisplayName("Only the reporter or an admin can verify / reopen / give feedback")
    void reporterRules() {
        assertDoesNotThrow(() -> policy.assertIsReporterOrAdmin(reporter, incident));
        assertThrows(AccessDeniedException.class, () -> policy.assertIsReporterOrAdmin(assignee, incident));
    }

    @Test
    @DisplayName("Internal notes are hidden from employees")
    void internalNotes() {
        assertFalse(policy.seesInternalNotes(reporter));
        assertTrue(policy.seesInternalNotes(assignee));
    }
}
