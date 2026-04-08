package com.glycowatch.auth.dto;

import com.glycowatch.auth.model.UserRole;

public record UserSummaryDto(
        Long id,
        String email,
        UserRole role
) {
}



