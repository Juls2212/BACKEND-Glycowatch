package com.glycowatch.backend.auth.dto;

public record LoginResponseDto(
        AuthTokensDto tokens,
        UserSummaryDto user
) {
}


