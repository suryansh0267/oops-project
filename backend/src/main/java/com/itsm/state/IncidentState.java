package com.itsm.state;

import com.itsm.enums.IncidentStatus;

import java.util.Set;

/**
 * State pattern: each lifecycle stage of an incident is an object that knows
 * which stages may legally follow it and how it behaves for SLA purposes.
 * IncidentService asks the current state instead of hard-coding a switch,
 * so adding a stage (e.g. ON_HOLD) means adding one class.
 */
public interface IncidentState {

    IncidentStatus status();

    Set<IncidentStatus> allowedTransitions();

    default boolean canTransitionTo(IncidentStatus target) {
        return allowedTransitions().contains(target);
    }

    /** True while the incident still needs work and its SLA clock is running. */
    boolean isOpen();

    /** True once a technician has responded (acknowledged or later). */
    boolean isResponded();

    /** Human-friendly label shown in the UI. */
    String label();
}
