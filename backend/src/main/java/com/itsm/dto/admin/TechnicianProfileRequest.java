package com.itsm.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** All fields optional on update; null means "leave unchanged". */
public record TechnicianProfileRequest(
        @Size(max = 20) List<@NotBlank @Size(max = 40) String> skills,
        @Size(max = 120) String specialization,
        @Min(1) @Max(50) Integer maxActiveIncidents,
        Boolean available) {
}
