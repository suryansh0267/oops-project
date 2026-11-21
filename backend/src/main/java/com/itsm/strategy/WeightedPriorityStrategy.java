package com.itsm.strategy;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import org.springframework.stereotype.Component;

@Component("weightedPriority")
public class WeightedPriorityStrategy implements PriorityCalculationStrategy {

    @Override
    public String getKey() {
        return "weightedPriority";
    }

    @Override
    public String getDisplayName() {
        return "Weighted score";
    }

    @Override
    public String getDescription() {
        return "Score = 0.4 impact + 0.35 urgency + 0.25 criticality. 2.5+ is P1, 1.8+ P2, 1.2+ P3, else P4.";
    }

    @Override
    public Priority calculatePriority(Impact impact, Urgency urgency, int businessCriticality) {
        double score = (impact.getValue() * 0.4) + (urgency.getValue() * 0.35) + (businessCriticality * 0.25);

        if (score >= 2.5) {
            return Priority.P1_CRITICAL;
        } else if (score >= 1.8) {
            return Priority.P2_HIGH;
        } else if (score >= 1.2) {
            return Priority.P3_MEDIUM;
        } else {
            return Priority.P4_LOW;
        }
    }
}
