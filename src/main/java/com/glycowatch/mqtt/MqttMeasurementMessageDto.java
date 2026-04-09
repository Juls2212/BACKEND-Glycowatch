package com.glycowatch.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MqttMeasurementMessageDto(
        @NotBlank(message = "Device identifier is required.")
        @Size(max = 255, message = "Device identifier cannot exceed 255 characters.")
        String deviceIdentifier,

        @NotBlank(message = "API key is required.")
        @Size(max = 255, message = "API key cannot exceed 255 characters.")
        String apiKey,

        @NotNull(message = "Glucose value is required.")
        @DecimalMin(value = "1.00", message = "Glucose value must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Glucose value must be <= 1000.")
        @Digits(integer = 4, fraction = 2, message = "Glucose value must have up to 4 integer digits and 2 decimal places.")
        @JsonProperty("glucoseMgDl")
        BigDecimal glucoseMgDl,

        @NotNull(message = "Measured timestamp is required.")
        @PastOrPresent(message = "Measured timestamp cannot be in the future.")
        Instant measuredAt,

        @Size(max = 255, message = "Source event id cannot exceed 255 characters.")
        String sourceEventId,

        @Size(max = 100, message = "Origin cannot exceed 100 characters.")
        String origin
) {
    public MqttMeasurementMessageDto {
        deviceIdentifier = trimToNull(deviceIdentifier);
        apiKey = trimToNull(apiKey);
        sourceEventId = trimToNull(sourceEventId);
        origin = trimToNull(origin);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
