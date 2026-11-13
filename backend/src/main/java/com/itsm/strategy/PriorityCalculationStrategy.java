package com.itsm.strategy;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;

public interface PriorityCalculationStrategy {

    Priority calculatePriority(Impact impact, Urgency urgency, int businessCriticality);
}
