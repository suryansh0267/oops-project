package com.itsm.dto;

/** A resolution that worked for a similar past incident. */
public record ResolutionSuggestion(Long incidentId, String incidentNumber, String title,
                                   String resolutionNotes, double score) {
}
