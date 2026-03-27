package com.glycowatch.backend.interfaces.dto.auth;

public record RegisterResponseDto(
        Long userId,
        String email
) {
}

