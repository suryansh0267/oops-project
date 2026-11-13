package com.itsm.dto;

import com.itsm.enums.Priority;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SLAPolicyRequest {

    private Priority priority;

    @Min(1)
    private int responseTimeMinutes;

    @Min(1)
    private int resolutionTimeMinutes;

    private String description;
}
