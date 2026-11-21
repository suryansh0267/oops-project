package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolveRequest {

    @NotBlank(message = "Describe how the incident was resolved")
    @Size(min = 10, max = 4000, message = "Resolution notes must be 10-4000 characters")
    private String resolutionNotes;
}
