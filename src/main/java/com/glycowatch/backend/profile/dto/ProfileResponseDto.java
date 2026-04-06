package com.glycowatch.backend.profile.dto;

import com.glycowatch.backend.profile.model.DiabetesType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfileResponseDto(
        Long userId,
        String email,
        String fullName,
        LocalDate birthDate,
        BigDecimal hypoglycemiaThreshold,
        BigDecimal hyperglycemiaThreshold,
        String timezone,
        BigDecimal weightKg,
        BigDecimal heightCm,
        DiabetesType diabetesType
) {
}

