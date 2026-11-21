package com.itsm.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record StrategyRequest(@NotBlank String strategy) {
}
