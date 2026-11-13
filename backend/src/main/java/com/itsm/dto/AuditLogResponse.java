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
public class AuditLogResponse {

    private Long id;
    private String action;
    private String previousValue;
    private String newValue;
    private String description;
    private String performedByName;
    private LocalDateTime timestamp;
    private String incidentNumber;
}
