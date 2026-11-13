package com.itsm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ITServiceRequest {

    @NotBlank
    private String name;

    private String description;

    private String category;

    @Min(1)
    @Max(5)
    private int businessCriticality;

    private String supportContact;
}
