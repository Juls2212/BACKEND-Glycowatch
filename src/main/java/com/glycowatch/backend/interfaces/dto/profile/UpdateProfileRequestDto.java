package com.glycowatch.backend.interfaces.dto.profile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@ValidThresholdRange
public record UpdateProfileRequestDto(
        @Size(min = 1, max = 255, message = "Full name must contain between 1 and 255 characters.")
        String fullName,

        @Past(message = "Birth date must be in the past.")
        LocalDate birthDate,

        @NotNull(message = "Hypoglycemia threshold is required.")
        @DecimalMin(value = "1.00", message = "Hypoglycemia threshold must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Hypoglycemia threshold must be <= 1000.")
        BigDecimal hypoglycemiaThreshold,

        @NotNull(message = "Hyperglycemia threshold is required.")
        @DecimalMin(value = "1.00", message = "Hyperglycemia threshold must be >= 1.")
        @DecimalMax(value = "1000.00", message = "Hyperglycemia threshold must be <= 1000.")
        BigDecimal hyperglycemiaThreshold,

        @Size(min = 1, max = 100, message = "Timezone must contain between 1 and 100 characters.")
        String timezone,

        @DecimalMin(value = "1.00", message = "Weight must be >= 1 kg.")
        @DecimalMax(value = "500.00", message = "Weight must be <= 500 kg.")
        BigDecimal weightKg,

        @DecimalMin(value = "30.00", message = "Height must be >= 30 cm.")
        @DecimalMax(value = "300.00", message = "Height must be <= 300 cm.")
        BigDecimal heightCm
) {
}
