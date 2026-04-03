package com.glycowatch.backend.measurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LatestMeasurementResponseDto(
        BigDecimal glucoseValue,
        String unit,
        Instant measuredAt
) {
}


