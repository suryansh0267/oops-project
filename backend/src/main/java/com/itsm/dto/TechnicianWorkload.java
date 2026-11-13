package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnicianWorkload {

    private Long technicianId;
    private String technicianName;
    private int activeIncidents;
    private int maxIncidents;
    private int totalResolved;
    private double avgResolutionMinutes;
    private List<String> skills;
}
