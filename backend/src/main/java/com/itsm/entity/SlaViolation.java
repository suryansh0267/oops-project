package com.itsm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Permanent record of an SLA breach, written by the SLA monitor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sla_violations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"incident_id", "violationType"}))
public class SlaViolation {

    public enum Type { RESPONSE, RESOLUTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type violationType;

    private LocalDateTime deadline;

    private LocalDateTime detectedAt;

    /** Technician responsible at the time of the breach (may be null if unassigned). */
    private String technicianName;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
