package com.itsm.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Draft incident text sent while the employee is still typing (classification / duplicate check). */
@Data
public class TextRequest {

    @Size(max = 150)
    private String title;

    @Size(max = 5000)
    private String description;

    private Long affectedServiceId;

    private String category;
}
