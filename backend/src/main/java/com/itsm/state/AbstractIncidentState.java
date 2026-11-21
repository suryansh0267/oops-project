package com.itsm.state;

import com.itsm.enums.IncidentStatus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

abstract class AbstractIncidentState implements IncidentState {

    private final IncidentStatus status;
    private final Set<IncidentStatus> allowed;
    private final String label;

    protected AbstractIncidentState(IncidentStatus status, String label, IncidentStatus... next) {
        this.status = status;
        this.label = label;
        this.allowed = next.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.of(next[0], next));
    }

    @Override
    public final IncidentStatus status() {
        return status;
    }

    @Override
    public final Set<IncidentStatus> allowedTransitions() {
        return allowed;
    }

    @Override
    public final String label() {
        return label;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isResponded() {
        return false;
    }
}
