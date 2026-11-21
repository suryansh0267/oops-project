package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class ClassifiedState extends AbstractIncidentState {

    ClassifiedState() {
        super(IncidentStatus.CLASSIFIED, "Classified", IncidentStatus.ASSIGNED);
    }
}
