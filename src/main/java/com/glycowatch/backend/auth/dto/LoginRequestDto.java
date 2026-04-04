package com.glycowatch.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequestDto(
        @Email(message = "Email format is invalid.")
        @NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email cannot exceed 255 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must contain between 8 and 100 characters.")
        String password
) {
    public LoginRequestDto {
        email = normalizeEmail(email);
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}


