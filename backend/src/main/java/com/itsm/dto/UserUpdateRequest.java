package com.itsm.dto;

import com.itsm.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin edit of a user. Password is optional: blank keeps the current one. */
@Data
public class UserUpdateRequest {

    @NotBlank
    @Size(max = 80)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @Size(max = 60)
    private String department;

    @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "Phone number looks invalid")
    private String phoneNumber;

    private UserRole role;

    @Pattern(regexp = "^$|^.{8,72}$", message = "Password must be 8-72 characters")
    private String password;
}
