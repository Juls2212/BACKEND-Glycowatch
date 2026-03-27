package com.glycowatch.backend.interfaces.dto.auth;

import com.glycowatch.backend.domain.user.UserRole;

public record UserSummaryDto(
        Long id,
        String email,
        UserRole role
) {
}

