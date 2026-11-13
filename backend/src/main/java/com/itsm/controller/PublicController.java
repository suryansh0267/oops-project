package com.itsm.controller;

import com.itsm.service.ITServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ITServiceService itServiceService;
    private final com.itsm.service.IncidentCategoryService incidentCategoryService;

    @GetMapping("/services")
    public ResponseEntity<?> getActiveServices() {
        return ResponseEntity.ok(itServiceService.getActiveServices());
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getActiveCategories() {
        return ResponseEntity.ok(incidentCategoryService.getActiveCategories());
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now()));
    }
}
