package com.itsm.strategy;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriorityStrategyManager extends StrategyRegistry<PriorityCalculationStrategy> {

    public PriorityStrategyManager(List<PriorityCalculationStrategy> strategies) {
        super(strategies, "defaultPriority");
    }

    public Priority calculate(Impact impact, Urgency urgency, int businessCriticality, Integer affectedUsers) {
        return active().calculatePriority(impact, urgency, businessCriticality, affectedUsers);
    }
}
