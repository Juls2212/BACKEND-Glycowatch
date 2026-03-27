package com.glycowatch.backend.interfaces.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardResponseDto(
        LatestMeasurementDto latestMeasurement,
        BigDecimal averageGlucose,
        BigDecimal minGlucose,
        BigDecimal maxGlucose,
        long alertsCount
) {
    public record LatestMeasurementDto(
            BigDecimal glucoseValue,
            String unit,
            Instant measuredAt
    ) {
    }
}

