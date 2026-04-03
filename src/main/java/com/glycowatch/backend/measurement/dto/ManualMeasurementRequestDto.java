package com.glycowatch.backend.measurement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.Instant;

public record ManualMeasurementRequestDto(
        @NotNull(message = "Glucose value is required.")
        @DecimalMin(value = "1.00", message = "Glucose value must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Glucose value must be <= 1000.")
        BigDecimal glucoseValue,

        @NotBlank(message = "Unit is required.")
        String unit,

        @NotNull(message = "Measured timestamp is required.")
        @PastOrPresent(message = "Measured timestamp cannot be in the future.")
        Instant measuredAt
) {
}

