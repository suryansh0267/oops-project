package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class InProgressState extends AbstractIncidentState {

    InProgressState() {
        super(IncidentStatus.IN_PROGRESS, "In progress", IncidentStatus.RESOLVED, IncidentStatus.ASSIGNED);
    }

    @Override
    public boolean isResponded() {
        return true;
    }
}
