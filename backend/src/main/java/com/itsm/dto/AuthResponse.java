package com.itsm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private Long id;

    @Builder.Default
    private String tokenType = "Bearer";

    private String username;

    private String email;

    private String fullName;

    private String role;
}
