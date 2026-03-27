package com.glycowatch.backend.interfaces.dto.auth;

public record AuthTokensDto(
        String accessToken,
        String refreshToken
) {
}

