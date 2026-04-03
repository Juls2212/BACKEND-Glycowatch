package com.glycowatch.backend.auth.dto;

import com.glycowatch.backend.auth.model.UserRole;

public record UserSummaryDto(
        Long id,
        String email,
        UserRole role
) {
}


