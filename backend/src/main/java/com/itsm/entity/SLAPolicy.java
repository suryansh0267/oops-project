package com.itsm.entity;

import com.itsm.enums.Priority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sla_policies")
public class SLAPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private int responseTimeMinutes;

    private int resolutionTimeMinutes;

    private String description;

    @Builder.Default
    private boolean active = true;
}
