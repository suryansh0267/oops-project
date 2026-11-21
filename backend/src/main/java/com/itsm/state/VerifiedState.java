package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class VerifiedState extends AbstractIncidentState {

    VerifiedState() {
        super(IncidentStatus.VERIFIED, "Verified", IncidentStatus.CLOSED);
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
