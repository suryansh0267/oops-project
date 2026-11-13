package com.itsm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FeedbackRequest {

    private String feedback;

    @Min(0)
    @Max(5)
    private int rating;
}
