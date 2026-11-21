package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for "request more information", "reply", "reopen" and similar free-text actions. */
@Data
public class MessageRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000)
    private String message;
}
