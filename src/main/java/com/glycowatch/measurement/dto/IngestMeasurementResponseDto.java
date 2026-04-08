package com.glycowatch.measurement.dto;

public record IngestMeasurementResponseDto(
        Long measurementId,
        boolean isValid,
        String invalidReason
) {
}



