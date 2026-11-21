package com.itsm.dto;

import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IncidentRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be 5-150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    /** Optional. When blank the system suggests a category from the text. */
    private String category;

    private Impact impact;

    private Urgency urgency;

    private Long affectedServiceId;

    @Min(value = 1, message = "Affected users must be at least 1")
    @Max(value = 100000, message = "Affected users looks too large")
    private Integer affectedUsers;
}
