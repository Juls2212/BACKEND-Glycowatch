package com.glycowatch.auth.dto;

public record AuthTokensDto(
        String accessToken,
        String refreshToken
) {
}



