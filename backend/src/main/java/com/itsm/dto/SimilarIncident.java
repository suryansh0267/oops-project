package com.itsm.dto;

import java.time.LocalDateTime;

/** An incident that looks related to another one, with the similarity score (0-1). */
public record SimilarIncident(Long id, String incidentNumber, String title, String status,
                              String serviceName, double score, String reason, LocalDateTime createdAt) {
}
