package com.itsm.dto;

import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncidentRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private IncidentCategory category;

    private Impact impact;

    private Urgency urgency;

    private Long affectedServiceId;
}
