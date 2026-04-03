package com.glycowatch.backend.measurement.service;

import com.glycowatch.backend.alert.service.AlertService;
import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.measurement.model.MeasurementOrigin;
import com.glycowatch.backend.auth.model.UserEntity;
import com.glycowatch.backend.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.auth.repository.UserRepository;
import com.glycowatch.backend.measurement.dto.IngestMeasurementResponseDto;
import com.glycowatch.backend.measurement.dto.ManualMeasurementRequestDto;
import com.glycowatch.backend.common.exception.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeasurementManualServiceImpl implements MeasurementManualService {

    private final UserRepository userRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final MeasurementValidationSupport validationSupport;
    private final AlertService alertService;

    @Override
    @Transactional
    public IngestMeasurementResponseDto createManualMeasurement(String authenticatedEmail, ManualMeasurementRequestDto request) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        validationSupport.validateMeasuredAtNotInFuture(request.measuredAt());

        String normalizedUnit = validationSupport.normalizeUnit(request.unit());
        String deduplicationHash = validationSupport.calculateDeduplicationHash(
                "manual-user-" + user.getId(),
                request.glucoseValue(),
                normalizedUnit,
                request.measuredAt()
        );

        if (glucoseMeasurementRepository.existsByDeduplicationHash(deduplicationHash)) {
            throw new ApiException("DUPLICATE_MEASUREMENT", "Measurement was already registered.", HttpStatus.CONFLICT);
        }

        MeasurementValidationSupport.ValidationResult validationResult =
                validationSupport.validateMeasurement(request.glucoseValue(), normalizedUnit);

        Instant now = Instant.now();
        GlucoseMeasurementEntity measurement = GlucoseMeasurementEntity.builder()
                .user(user)
                .device(null)
                .glucoseValue(request.glucoseValue())
                .unit(normalizedUnit)
                .measuredAt(request.measuredAt())
                .receivedAt(now)
                .isValid(validationResult.valid())
                .invalidReason(validationResult.invalidReason())
                .deduplicationHash(deduplicationHash)
                .origin(MeasurementOrigin.MANUAL)
                .createdAt(now)
                .createdBy(user.getEmail())
                .build();

        GlucoseMeasurementEntity saved = glucoseMeasurementRepository.save(measurement);
        alertService.generateForMeasurement(saved);

        return new IngestMeasurementResponseDto(saved.getId(), Boolean.TRUE.equals(saved.getIsValid()), saved.getInvalidReason());
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }
}

