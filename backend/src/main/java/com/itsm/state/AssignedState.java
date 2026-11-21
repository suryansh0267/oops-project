package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class AssignedState extends AbstractIncidentState {

    AssignedState() {
        super(IncidentStatus.ASSIGNED, "Assigned", IncidentStatus.ACKNOWLEDGED, IncidentStatus.ASSIGNED);
    }
}
