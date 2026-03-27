package com.glycowatch.backend.interfaces.dto.measurement;

import java.math.BigDecimal;
import java.time.Instant;

public record LatestMeasurementResponseDto(
        BigDecimal glucoseValue,
        String unit,
        Instant measuredAt
) {
}

