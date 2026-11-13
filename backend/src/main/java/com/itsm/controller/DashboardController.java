package com.itsm.controller;

import com.itsm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/technician-workloads")
    public ResponseEntity<?> getTechnicianWorkloads() {
        return ResponseEntity.ok(dashboardService.getTechnicianWorkloads());
    }

    @GetMapping("/service-analytics")
    public ResponseEntity<?> getServiceAnalytics() {
        return ResponseEntity.ok(dashboardService.getServiceAnalytics());
    }
}
