package com.itsm.strategy;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import org.springframework.stereotype.Component;

@Component("defaultPriority")
public class DefaultPriorityStrategy implements PriorityCalculationStrategy {

    @Override
    public String getKey() {
        return "defaultPriority";
    }

    @Override
    public String getDisplayName() {
        return "Multiplicative score";
    }

    @Override
    public String getDescription() {
        return "Score = impact x urgency x business criticality (1-45). 27+ is P1, 12+ P2, 6+ P3, else P4.";
    }

    @Override
    public Priority calculatePriority(Impact impact, Urgency urgency, int businessCriticality) {
        int score = impact.getValue() * urgency.getValue() * businessCriticality;

        if (score >= 27) {
            return Priority.P1_CRITICAL;
        } else if (score >= 12) {
            return Priority.P2_HIGH;
        } else if (score >= 6) {
            return Priority.P3_MEDIUM;
        } else {
            return Priority.P4_LOW;
        }
    }
}
