package com.glycowatch.backend.interfaces.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public record ChartPointDto(
        Instant measuredAt,
        BigDecimal glucoseValue
) {
}

