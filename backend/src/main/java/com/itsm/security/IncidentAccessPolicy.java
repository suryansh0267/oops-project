package com.itsm.security;

import com.itsm.entity.Incident;
import com.itsm.entity.User;
import com.itsm.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Object-level authorisation rules for incidents, kept in one class so they are
 * easy to read, test and defend:
 * <ul>
 *   <li>Employees see and act only on incidents they reported.</li>
 *   <li>Technicians can see every incident but only work on ones assigned to them.</li>
 *   <li>Admins can do everything.</li>
 * </ul>
 */
@Component
public class IncidentAccessPolicy {

    public boolean canView(User user, Incident incident) {
        return switch (user.getRole()) {
            case ADMIN, TECHNICIAN -> true;
            case EMPLOYEE -> isReporter(user, incident);
        };
    }

    public void assertCanView(User user, Incident incident) {
        if (!canView(user, incident)) {
            throw new AccessDeniedException("You can only view incidents you reported");
        }
    }

    /** Acknowledge, investigate, resolve, request information. */
    public void assertCanWork(User user, Incident incident) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (user.getRole() == UserRole.TECHNICIAN && isAssignee(user, incident)) return;
        throw new AccessDeniedException("Only the assigned technician or an admin can do this");
    }

    /** Verify, reopen, reply to questions, give feedback. */
    public void assertIsReporterOrAdmin(User user, Incident incident) {
        if (user.getRole() == UserRole.ADMIN || isReporter(user, incident)) return;
        throw new AccessDeniedException("Only the person who reported this incident can do this");
    }

    /** Close: the reporter, the assigned technician or an admin. */
    public void assertCanClose(User user, Incident incident) {
        if (user.getRole() == UserRole.ADMIN || isReporter(user, incident) || isAssignee(user, incident)) return;
        throw new AccessDeniedException("You cannot close this incident");
    }

    public boolean seesInternalNotes(User user) {
        return user.getRole() != UserRole.EMPLOYEE;
    }

    private static boolean isReporter(User user, Incident incident) {
        return incident.getReporter() != null && Objects.equals(incident.getReporter().getId(), user.getId());
    }

    private static boolean isAssignee(User user, Incident incident) {
        return incident.getAssignedTechnician() != null
                && Objects.equals(incident.getAssignedTechnician().getId(), user.getId());
    }
}
