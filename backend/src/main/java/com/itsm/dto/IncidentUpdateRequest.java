package com.itsm.dto;

import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Urgency;
import lombok.Data;

@Data
public class IncidentUpdateRequest {

    private IncidentCategory category;

    private Impact impact;

    private Urgency urgency;

    private String additionalInfo;
}
