package com.glycowatch.backend.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ChartPointDto(
        Instant measuredAt,
        BigDecimal glucoseValue
) {
}


