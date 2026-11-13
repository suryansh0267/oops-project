package com.itsm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "technician_profiles")
public class TechnicianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ElementCollection
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_profile_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "technician_certifications", joinColumns = @JoinColumn(name = "technician_profile_id"))
    @Column(name = "certification")
    @Builder.Default
    private List<String> certifications = new ArrayList<>();

    @Builder.Default
    private int maxActiveIncidents = 10;

    @Builder.Default
    private int currentActiveIncidents = 0;

    @Builder.Default
    private int totalResolvedIncidents = 0;

    @Builder.Default
    private double avgResolutionTimeMinutes = 0.0;

    @Builder.Default
    private boolean available = true;

    private String specialization;
}
