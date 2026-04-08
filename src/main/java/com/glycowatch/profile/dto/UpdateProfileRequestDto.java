package com.glycowatch.profile.dto;

import com.glycowatch.profile.model.DiabetesType;
import com.glycowatch.profile.validation.ValidThresholdRange;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@ValidThresholdRange
public record UpdateProfileRequestDto(
        @Size(max = 255, message = "Full name cannot exceed 255 characters.")
        String fullName,

        @Past(message = "Birth date must be in the past.")
        LocalDate birthDate,

        @NotNull(message = "Hypoglycemia threshold is required.")
        @DecimalMin(value = "1.00", message = "Hypoglycemia threshold must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Hypoglycemia threshold must be <= 1000.")
        @Digits(integer = 4, fraction = 2, message = "Hypoglycemia threshold must have up to 4 integer digits and 2 decimal places.")
        BigDecimal hypoglycemiaThreshold,

        @NotNull(message = "Hyperglycemia threshold is required.")
        @DecimalMin(value = "1.00", message = "Hyperglycemia threshold must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Hyperglycemia threshold must be <= 1000.")
        @Digits(integer = 4, fraction = 2, message = "Hyperglycemia threshold must have up to 4 integer digits and 2 decimal places.")
        BigDecimal hyperglycemiaThreshold,

        @Size(max = 100, message = "Timezone cannot exceed 100 characters.")
        String timezone,

        @DecimalMin(value = "1.00", message = "Weight must be >= 1 kg.")
        @DecimalMax(value = "500.00", message = "Weight must be <= 500 kg.")
        @Digits(integer = 4, fraction = 2, message = "Weight must have up to 4 integer digits and 2 decimal places.")
        BigDecimal weightKg,

        @DecimalMin(value = "30.00", message = "Height must be >= 30 cm.")
        @DecimalMax(value = "300.00", message = "Height must be <= 300 cm.")
        @Digits(integer = 3, fraction = 2, message = "Height must have up to 3 integer digits and 2 decimal places.")
        BigDecimal heightCm,

        @NotNull(message = "Diabetes type is required.")
        DiabetesType diabetesType
) {
    public UpdateProfileRequestDto {
        fullName = trimToNull(fullName);
        timezone = trimToNull(timezone);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


