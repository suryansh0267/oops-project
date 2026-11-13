package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveRequest {

    @NotBlank
    private String resolutionNotes;
}
