package com.glycowatch.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequestDto(
        @Email(message = "Email format is invalid.")
        @NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email cannot exceed 255 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must contain between 8 and 100 characters.")
        String password,

        @NotBlank(message = "Full name is required.")
        @Size(max = 255, message = "Full name cannot exceed 255 characters.")
        String fullName
) {
    public RegisterRequestDto {
        email = normalizeEmail(email);
        fullName = trimToNull(fullName);
    }

    private static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


