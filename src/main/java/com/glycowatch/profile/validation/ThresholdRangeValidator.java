package com.glycowatch.profile.validation;

import com.glycowatch.profile.dto.UpdateProfileRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class ThresholdRangeValidator implements ConstraintValidator<ValidThresholdRange, UpdateProfileRequestDto> {

    @Override
    public boolean isValid(UpdateProfileRequestDto value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal hypo = value.hypoglycemiaThreshold();
        BigDecimal hyper = value.hyperglycemiaThreshold();
        if (hypo == null || hyper == null) {
            return true;
        }
        return hypo.compareTo(hyper) < 0;
    }
}



