package com.itsm.strategy;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;

public interface PriorityCalculationStrategy extends NamedStrategy {

    /** Users affected at which priority is raised one level. */
    int MANY_USERS = 50;
    /** Users affected at which priority is raised two levels. */
    int MASS_OUTAGE_USERS = 500;

    Priority calculatePriority(Impact impact, Urgency urgency, int businessCriticality);

    /**
     * Template method: the strategy computes a base priority, then the number of
     * affected users can raise it (never lower it).
     */
    default Priority calculatePriority(Impact impact, Urgency urgency, int businessCriticality, Integer affectedUsers) {
        return raiseForAffectedUsers(calculatePriority(impact, urgency, businessCriticality), affectedUsers);
    }

    static Priority raiseForAffectedUsers(Priority base, Integer affectedUsers) {
        if (affectedUsers == null || affectedUsers < MANY_USERS) {
            return base;
        }
        int steps = affectedUsers >= MASS_OUTAGE_USERS ? 2 : 1;
        int index = Math.max(0, base.ordinal() - steps);
        return Priority.values()[index];
    }
}
