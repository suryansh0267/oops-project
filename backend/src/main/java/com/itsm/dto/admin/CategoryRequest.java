package com.itsm.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategoryRequest(
        @NotBlank @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9 _-]*$", message = "Use letters, numbers, spaces, - or _")
        String name,
        @Size(max = 255) String description,
        @Size(max = 40) List<@NotBlank @Size(max = 40) String> keywords) {
}
