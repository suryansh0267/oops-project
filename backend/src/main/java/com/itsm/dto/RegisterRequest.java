package com.itsm.dto;

import com.itsm.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can use letters, numbers, dot, dash and underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 80)
    private String fullName;

    @Size(max = 60)
    private String department;

    @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "Phone number looks invalid")
    private String phoneNumber;

    /** Only honoured when an admin creates the user. Ignored on self-registration. */
    private UserRole role;
}
