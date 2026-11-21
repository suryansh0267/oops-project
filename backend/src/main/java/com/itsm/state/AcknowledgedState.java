package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class AcknowledgedState extends AbstractIncidentState {

    AcknowledgedState() {
        super(IncidentStatus.ACKNOWLEDGED, "Acknowledged", IncidentStatus.IN_PROGRESS, IncidentStatus.ASSIGNED);
    }

    @Override
    public boolean isResponded() {
        return true;
    }
}
