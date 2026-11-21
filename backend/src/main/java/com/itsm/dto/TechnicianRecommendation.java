package com.itsm.dto;

import com.itsm.intelligence.TechnicianScorer;

import java.util.List;

public record TechnicianRecommendation(Long technicianId, String name, List<String> skills,
                                       int activeIncidents, int maxIncidents, boolean available,
                                       TechnicianScorer.Score score) {
}
