package com.itsm.repository;

import com.itsm.entity.Incident;
import com.itsm.entity.User;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Composable query criteria for incident search (Specification pattern on top of the Repository).
 * Each method returns null when its filter is not set; Spring Data ignores null specs.
 */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    /** Staff-only frustration criterion, combined with existing filters. */
    public static Specification<Incident> frustration(String level) {
        if (level == null || level.isBlank()) return null;
        return (root, query, cb) -> {
            if ("FLAGGED".equalsIgnoreCase(level)) return root.get("frustrationLevel")
                    .in(com.itsm.intelligence.frustration.FrustrationLevel.FRUSTRATED,
                            com.itsm.intelligence.frustration.FrustrationLevel.CRITICAL);
            return cb.equal(root.get("frustrationLevel"),
                    com.itsm.intelligence.frustration.FrustrationLevel.valueOf(level.toUpperCase(Locale.ROOT)));
        };
    }

    public static Specification<Incident> keyword(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("incidentNumber")), like));
    }

    public static Specification<Incident> status(IncidentStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> priority(Priority priority) {
        return priority == null ? null : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> category(String category) {
        if (category == null || category.isBlank()) return null;
        return (root, query, cb) -> cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase(Locale.ROOT));
    }

    public static Specification<Incident> service(Long serviceId) {
        return serviceId == null ? null : (root, query, cb) -> cb.equal(root.get("affectedService").get("id"), serviceId);
    }

    public static Specification<Incident> technician(Long technicianId) {
        return technicianId == null ? null : (root, query, cb) -> cb.equal(root.get("assignedTechnician").get("id"), technicianId);
    }

    public static Specification<Incident> reporter(User reporter) {
        return reporter == null ? null : (root, query, cb) -> cb.equal(root.get("reporter"), reporter);
    }

    public static Specification<Incident> createdFrom(LocalDateTime from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Incident> createdTo(LocalDateTime to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
