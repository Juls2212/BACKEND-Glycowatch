package com.glycowatch.auth.dto;

public record LoginResponseDto(
        AuthTokensDto tokens,
        UserSummaryDto user
) {
}



