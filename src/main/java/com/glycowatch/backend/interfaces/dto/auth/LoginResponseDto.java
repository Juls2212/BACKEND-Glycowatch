package com.glycowatch.backend.interfaces.dto.auth;

public record LoginResponseDto(
        AuthTokensDto tokens,
        UserSummaryDto user
) {
}

