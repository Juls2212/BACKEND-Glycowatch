package com.glycowatch.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken
) {
    public RefreshTokenRequestDto {
        refreshToken = trimToNull(refreshToken);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



