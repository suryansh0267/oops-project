package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceAnalytics {

    private Long serviceId;
    private String serviceName;
    private int businessCriticality;
    private long totalIncidents;
    private long openIncidents;
    private long resolvedIncidents;
    private long slaViolations;
}
