package com.glycowatch.measurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementResponseDto(
        Long id,
        BigDecimal glucoseValue,
        String unit,
        Instant measuredAt,
        boolean isValid,
        String invalidReason,
        Long deviceId
) {
}



