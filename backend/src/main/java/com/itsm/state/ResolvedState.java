package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class ResolvedState extends AbstractIncidentState {

    ResolvedState() {
        super(IncidentStatus.RESOLVED, "Resolved", IncidentStatus.VERIFIED, IncidentStatus.CLOSED, IncidentStatus.IN_PROGRESS);
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isResponded() {
        return true;
    }
}
