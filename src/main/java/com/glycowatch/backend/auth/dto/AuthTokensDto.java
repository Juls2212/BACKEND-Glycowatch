package com.glycowatch.backend.auth.dto;

public record AuthTokensDto(
        String accessToken,
        String refreshToken
) {
}


