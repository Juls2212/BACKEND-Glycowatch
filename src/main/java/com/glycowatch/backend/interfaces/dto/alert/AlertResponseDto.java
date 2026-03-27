package com.glycowatch.backend.interfaces.dto.alert;

import com.glycowatch.backend.domain.alert.AlertType;
import java.time.Instant;

public record AlertResponseDto(
        Long id,
        AlertType type,
        String message,
        boolean isRead,
        Instant readAt,
        Long measurementId,
        Instant createdAt
) {
}

