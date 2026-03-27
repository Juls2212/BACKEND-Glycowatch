package com.glycowatch.backend.interfaces.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @Email(message = "Email format is invalid.")
        @NotBlank(message = "Email is required.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must contain between 8 and 100 characters.")
        String password,

        @NotBlank(message = "Full name is required.")
        @Size(max = 255, message = "Full name cannot exceed 255 characters.")
        String fullName
) {
}

