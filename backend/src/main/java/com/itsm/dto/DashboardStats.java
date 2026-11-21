package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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
    private long closedIncidents;
    /** Open incidents currently past a deadline. */
    private long slaViolations;
    /** Open incidents more than 75% through their window. */
    private long slaAtRisk;
    /** Breaches recorded by the SLA monitor since the start. */
    private long recordedViolations;
    /** Share of finished incidents resolved within their deadline (0-100). */
    private double slaCompliancePercent;
    /** Mean minutes from report to resolution. */
    private double meanTimeToResolveMinutes;
    /** Mean minutes from report to first acknowledgement. */
    private double meanTimeToRespondMinutes;
    private double averageRating;
    private long unassignedIncidents;
    private Map<String, Long> incidentsByCategory;
    private Map<String, Long> incidentsByPriority;
    private Map<String, Long> incidentsByStatus;
    /** Created / resolved per day for the last 14 days. */
    private List<Map<String, Object>> dailyTrend;
}
