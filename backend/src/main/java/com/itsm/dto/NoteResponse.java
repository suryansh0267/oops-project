package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoteResponse {
    private Long id;
    private String content;
    /** NOTE, INFO_REQUEST or INFO_RESPONSE. */
    private String kind;
    private boolean internalNote;
    private String authorName;
    private String authorRole;
    private LocalDateTime createdAt;
}
