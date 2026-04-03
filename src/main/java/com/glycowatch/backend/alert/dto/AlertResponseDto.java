package com.glycowatch.backend.alert.dto;

import com.glycowatch.backend.alert.model.AlertType;
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


