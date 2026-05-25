package com.glycowatch.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequestDto(
        @Email(message = "Email format is invalid.")
        @jakarta.validation.constraints.NotBlank(message = "Email is required.")
        @Size(max = 255, message = "Email cannot exceed 255 characters.")
        String email,

        String password,

        String passwordHash
) {
    public LoginRequestDto {
        email = normalizeEmail(email);
        password = normalizeCredential(password);
        passwordHash = normalizeCredential(passwordHash);
    }

    @AssertTrue(message = "Password hash or password is required.")
    public boolean hasCredential() {
        return resolvedPasswordInput() != null;
    }

    @AssertTrue(message = "Password hash or password must contain between 8 and 255 characters.")
    public boolean hasValidCredentialLength() {
        String credential = resolvedPasswordInput();
        return credential == null || (credential.length() >= 8 && credential.length() <= 255);
    }

    public String resolvedPasswordInput() {
        return passwordHash != null ? passwordHash : password;
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String normalizeCredential(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



