package com.glycowatch.measurement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record IngestMeasurementRequestDto(
        @NotNull(message = "Glucose value is required.")
        @DecimalMin(value = "1.00", message = "Glucose value must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Glucose value must be <= 1000.")
        @Digits(integer = 4, fraction = 2, message = "Glucose value must have up to 4 integer digits and 2 decimal places.")
        BigDecimal glucoseValue,

        @NotBlank(message = "Unit is required.")
        @Size(max = 20, message = "Unit cannot exceed 20 characters.")
        String unit,

        @NotNull(message = "Measured timestamp is required.")
        @PastOrPresent(message = "Measured timestamp cannot be in the future.")
        Instant measuredAt
) {
    public IngestMeasurementRequestDto {
        unit = trimToNull(unit);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



