package com.itsm.dto;

import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IncidentUpdateRequest {

    private String category;

    private Impact impact;

    private Urgency urgency;

    @Size(max = 2000)
    private String additionalInfo;
}
