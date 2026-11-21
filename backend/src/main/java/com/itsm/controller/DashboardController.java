package com.itsm.controller;

import com.itsm.dto.DashboardStats;
import com.itsm.dto.ServiceAnalytics;
import com.itsm.dto.TechnicianWorkload;
import com.itsm.service.DashboardService;
import com.itsm.intelligence.frustration.FrustrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Organisation-wide analytics. Restricted to technicians and admins in SecurityConfig. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final FrustrationService frustrationService;

    @GetMapping("/frustration")
    public Map<String, Object> frustration() { return frustrationService.dashboard(); }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return dashboardService.getDashboardStats();
    }

    @GetMapping("/technician-workloads")
    public List<TechnicianWorkload> workloads() {
        return dashboardService.getTechnicianWorkloads();
    }

    @GetMapping("/service-analytics")
    public List<ServiceAnalytics> services() {
        return dashboardService.getServiceAnalytics();
    }

    /** Possible outages: several open incidents on one service within an hour. */
    @GetMapping("/service-alerts")
    public List<Map<String, Object>> serviceAlerts() {
        return dashboardService.getServiceAlerts();
    }
}
