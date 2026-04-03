package com.glycowatch.backend.measurement.service;

import com.glycowatch.backend.common.exception.ApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MeasurementValidationSupport {

    private static final String DEFAULT_UNIT = "mg/dL";
    private static final BigDecimal PHYSIOLOGICAL_MIN = new BigDecimal("20.00");
    private static final BigDecimal PHYSIOLOGICAL_MAX = new BigDecimal("600.00");

    public String normalizeUnit(String rawUnit) {
        String normalized = rawUnit == null ? "" : rawUnit.trim();
        if (normalized.equalsIgnoreCase(DEFAULT_UNIT)) {
            return DEFAULT_UNIT;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public ValidationResult validateMeasurement(BigDecimal glucoseValue, String unit) {
        if (!DEFAULT_UNIT.equals(unit)) {
            return new ValidationResult(false, "Unsupported unit. Only mg/dL is accepted.");
        }
        if (glucoseValue.compareTo(PHYSIOLOGICAL_MIN) < 0 || glucoseValue.compareTo(PHYSIOLOGICAL_MAX) > 0) {
            return new ValidationResult(false, "Value is outside physiological range.");
        }
        return new ValidationResult(true, null);
    }

    public void validateMeasuredAtNotInFuture(Instant measuredAt) {
        if (measuredAt != null && measuredAt.isAfter(Instant.now())) {
            throw new ApiException(
                    "INVALID_MEASURED_AT",
                    "Measured timestamp cannot be in the future.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public String calculateDeduplicationHash(String ownerKey, BigDecimal glucoseValue, String unit, Instant measuredAt) {
        String payload = ownerKey + "|" + glucoseValue.stripTrailingZeros().toPlainString() + "|" + unit + "|" + measuredAt.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException("HASH_CALCULATION_FAILED", "Unable to calculate deduplication hash.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public record ValidationResult(boolean valid, String invalidReason) {
    }
}

