package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteRequest {

    @NotBlank(message = "Note cannot be empty")
    @Size(max = 2000)
    private String content;

    /** Internal notes are only visible to technicians and admins. */
    private boolean internalNote;
}
