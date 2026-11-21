package com.itsm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Read-only runtime configuration view; change values via environment + restart. */
@RestController @RequestMapping("/api/admin/frustration-config")
public class FrustrationConfigController {
    @Value("${app.frustration.enabled:true}") private boolean enabled;
    @Value("${app.frustration.annoyed:25}") private int annoyed;
    @Value("${app.frustration.frustrated:55}") private int frustrated;
    @Value("${app.frustration.critical:85}") private int critical;
    @Value("${app.frustration.auto-escalate:false}") private boolean autoEscalate;
    @Value("${app.frustration.llm-sentiment:false}") private boolean llmSentiment;
    @GetMapping
    public Map<String, Object> settings() {
        return Map.of("enabled", enabled, "annoyed", annoyed, "frustrated", frustrated,
                "critical", critical, "autoEscalate", autoEscalate, "llmSentiment", llmSentiment,
                "mode", "read-only: set environment variables and restart");
    }
}
