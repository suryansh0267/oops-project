package com.itsm.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(@NotBlank @Size(max = 1000) String message) { }
