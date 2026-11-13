package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IncidentResponse {

    private Long id;
    private String incidentNumber;
    private String title;
    private String description;
    private String category;
    private String impact;
    private String urgency;
    private String priority;
    private String status;
    private String reporterName;
    private Long reporterId;
    private String assignedTechnicianName;
    private Long assignedTechnicianId;
    private String affectedServiceName;
    private Long affectedServiceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private LocalDateTime respondedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String resolutionNotes;
    private String feedback;
    private int feedbackRating;
}
