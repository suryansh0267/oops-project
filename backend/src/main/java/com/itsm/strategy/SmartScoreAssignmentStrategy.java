package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import com.itsm.service.TechnicianRecommendationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Experience-aware assignment: ranks technicians by skill match, spare capacity,
 * overall experience and past work on the same service (see TechnicianScorer).
 */
@Component
public class SmartScoreAssignmentStrategy implements AssignmentStrategy {

    private final TechnicianRecommendationService recommendationService;

    public SmartScoreAssignmentStrategy(@Lazy TechnicianRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Override
    public String getKey() {
        return "smartScore";
    }

    @Override
    public String getDisplayName() {
        return "Smart score";
    }

    @Override
    public String getDescription() {
        return "Ranks technicians by skill match (40), spare capacity (30), experience (15) and history with the affected service (15).";
    }

    @Override
    public Optional<TechnicianProfile> assignTechnician(Incident incident, List<TechnicianProfile> availableTechnicians) {
        return recommendationService.best(incident, availableTechnicians);
    }
}
