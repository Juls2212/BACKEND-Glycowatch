package com.glycowatch.backend.application.measurement;

import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.domain.user.UserEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.AlertRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserRepository;
import com.glycowatch.backend.interfaces.dto.measurement.LatestMeasurementResponseDto;
import com.glycowatch.backend.interfaces.dto.measurement.MeasurementPageResponseDto;
import com.glycowatch.backend.interfaces.dto.measurement.MeasurementResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeasurementQueryServiceImpl implements MeasurementQueryService {

    private final UserRepository userRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public MeasurementPageResponseDto getMeasurements(
            String authenticatedEmail,
            int page,
            int size,
            LocalDate from,
            LocalDate to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException("INVALID_DATE_RANGE", "'from' must be earlier than or equal to 'to'.", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = resolveActiveUser(authenticatedEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "measuredAt"));
        Page<GlucoseMeasurementEntity> measurementsPage = queryMeasurements(user.getId(), from, to, pageable);

        return new MeasurementPageResponseDto(
                measurementsPage.getContent().stream().map(this::toMeasurementDto).toList(),
                measurementsPage.getTotalElements(),
                measurementsPage.getTotalPages(),
                measurementsPage.getNumber()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LatestMeasurementResponseDto getLatestMeasurement(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        GlucoseMeasurementEntity measurement = glucoseMeasurementRepository.findFirstByUserIdAndIsValidTrueOrderByMeasuredAtDesc(user.getId())
                .orElseThrow(() -> new ApiException("MEASUREMENT_NOT_FOUND", "No valid measurements were found.", HttpStatus.NOT_FOUND));

        return new LatestMeasurementResponseDto(
                measurement.getGlucoseValue(),
                measurement.getUnit(),
                measurement.getMeasuredAt()
        );
    }

    @Override
    @Transactional
    public void deleteMeasurement(String authenticatedEmail, Long measurementId) {
        UserEntity user = resolveActiveUser(authenticatedEmail);

        GlucoseMeasurementEntity measurement = glucoseMeasurementRepository.findByIdAndUserId(measurementId, user.getId())
                .orElseThrow(() -> new ApiException("MEASUREMENT_NOT_FOUND", "Measurement was not found.", HttpStatus.NOT_FOUND));

        alertRepository.deleteByMeasurementId(measurement.getId());
        glucoseMeasurementRepository.delete(measurement);
    }

    private Page<GlucoseMeasurementEntity> queryMeasurements(Long userId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null && to == null) {
            return glucoseMeasurementRepository.findByUserIdAndIsValidTrue(userId, pageable);
        }
        if (from != null && to == null) {
            Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
            return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqual(userId, fromInstant, pageable);
        }
        if (from == null) {
            Instant toInstant = to.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
            return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtLessThanEqual(userId, toInstant, pageable);
        }

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtBetween(userId, fromInstant, toInstant, pageable);
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private MeasurementResponseDto toMeasurementDto(GlucoseMeasurementEntity entity) {
        return new MeasurementResponseDto(
                entity.getId(),
                entity.getGlucoseValue(),
                entity.getUnit(),
                entity.getMeasuredAt(),
                Boolean.TRUE.equals(entity.getIsValid()),
                entity.getInvalidReason(),
                entity.getDevice() != null ? entity.getDevice().getId() : null
        );
    }
}
