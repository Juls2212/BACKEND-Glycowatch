package com.glycowatch.backend.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken
) {
}

