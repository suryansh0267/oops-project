package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStats {

    private long totalIncidents;
    private long openIncidents;
    private long criticalIncidents;
    private long resolvedIncidents;
    private long slaViolations;
    private long closedIncidents;
    private Map<String, Long> incidentsByCategory;
    private Map<String, Long> incidentsByPriority;
    private Map<String, Long> incidentsByStatus;
}
