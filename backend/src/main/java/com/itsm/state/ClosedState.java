package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class ClosedState extends AbstractIncidentState {

    ClosedState() {
        super(IncidentStatus.CLOSED, "Closed");
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
