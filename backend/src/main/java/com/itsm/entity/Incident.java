package com.itsm.entity;

import com.itsm.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String incidentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Category name, validated against the admin-managed incident_categories table.
     * Stored as plain text (same column type as the old enum) so admins can add
     * categories at runtime without a code change.
     */
    private String category;

    @Enumerated(EnumType.STRING)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_service_id")
    private ITService affectedService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_policy_id")
    private SLAPolicy slaPolicy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime responseDeadline;

    private LocalDateTime resolutionDeadline;

    private LocalDateTime respondedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private int feedbackRating;

    /** Number of users the reporter says are affected; feeds the priority calculation. */
    private Integer affectedUsers;

    /** When the "SLA approaching" warning was sent, so it is only sent once. */
    private LocalDateTime slaWarningSentAt;

    /** 0 = not escalated; 1 = reassigned/team lead; 2 = IT manager (admins) alerted. */
    private Integer escalationLevel;

    /** Staff-only heuristic flag, omitted from employee-facing incident DTOs. */
    @Enumerated(EnumType.STRING)
    private com.itsm.intelligence.frustration.FrustrationLevel frustrationLevel;
    @Column(length = 500)
    private String frustrationReasons;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IncidentNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public int escalationLevelOrZero() {
        return escalationLevel == null ? 0 : escalationLevel;
    }
}
