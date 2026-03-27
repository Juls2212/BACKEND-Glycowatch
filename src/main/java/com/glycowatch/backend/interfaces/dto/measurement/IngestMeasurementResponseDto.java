package com.glycowatch.backend.interfaces.dto.measurement;

public record IngestMeasurementResponseDto(
        Long measurementId,
        boolean isValid,
        String invalidReason
) {
}

