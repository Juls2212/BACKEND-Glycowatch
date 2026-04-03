package com.glycowatch.backend.measurement.dto;

public record IngestMeasurementResponseDto(
        Long measurementId,
        boolean isValid,
        String invalidReason
) {
}


