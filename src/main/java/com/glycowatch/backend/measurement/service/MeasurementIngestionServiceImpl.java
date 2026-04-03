package com.glycowatch.backend.measurement.service;

import com.glycowatch.backend.alert.service.AlertService;
import com.glycowatch.backend.device.model.DeviceEntity;
import com.glycowatch.backend.device.model.UserDeviceLinkEntity;
import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.measurement.model.MeasurementOrigin;
import com.glycowatch.backend.device.repository.DeviceRepository;
import com.glycowatch.backend.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.device.repository.UserDeviceLinkRepository;
import com.glycowatch.backend.measurement.dto.IngestMeasurementRequestDto;
import com.glycowatch.backend.measurement.dto.IngestMeasurementResponseDto;
import com.glycowatch.backend.common.exception.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeasurementIngestionServiceImpl implements MeasurementIngestionService {

    private final DeviceRepository deviceRepository;
    private final UserDeviceLinkRepository userDeviceLinkRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeasurementValidationSupport validationSupport;
    private final AlertService alertService;

    @Override
    @Transactional
    public IngestMeasurementResponseDto ingestMeasurement(
            String deviceIdentifierHeader,
            String deviceKeyHeader,
            IngestMeasurementRequestDto request
    ) {
        String deviceIdentifier = validateHeader(deviceIdentifierHeader, "X-DEVICE-ID");
        String deviceKey = validateHeader(deviceKeyHeader, "X-DEVICE-KEY");

        DeviceEntity device = resolveDevice(deviceIdentifier);

        if (!passwordEncoder.matches(deviceKey, device.getAuthKeyHash())) {
            throw new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED);
        }

        UserDeviceLinkEntity link = userDeviceLinkRepository.findTopByDeviceIdAndActiveTrueOrderByLinkedAtDesc(device.getId())
                .orElseThrow(() -> new ApiException("DEVICE_NOT_LINKED", "Device is not linked to an active user.", HttpStatus.FORBIDDEN));

        String normalizedUnit = validationSupport.normalizeUnit(request.unit());
        String deduplicationHash = validationSupport.calculateDeduplicationHash(
                "iot-device-" + device.getId(),
                request.glucoseValue(),
                normalizedUnit,
                request.measuredAt()
        );

        if (glucoseMeasurementRepository.existsByDeduplicationHash(deduplicationHash)) {
            throw new ApiException("DUPLICATE_MEASUREMENT", "Measurement was already ingested.", HttpStatus.CONFLICT);
        }

        MeasurementValidationSupport.ValidationResult validationResult =
                validationSupport.validateMeasurement(request.glucoseValue(), normalizedUnit);
        Instant now = Instant.now();

        GlucoseMeasurementEntity measurement = GlucoseMeasurementEntity.builder()
                .user(link.getUser())
                .device(device)
                .glucoseValue(request.glucoseValue())
                .unit(normalizedUnit)
                .measuredAt(request.measuredAt())
                .receivedAt(now)
                .isValid(validationResult.valid())
                .invalidReason(validationResult.invalidReason())
                .deduplicationHash(deduplicationHash)
                .origin(MeasurementOrigin.IOT)
                .createdAt(now)
                .createdBy(device.getUniqueIdentifier())
                .build();

        GlucoseMeasurementEntity saved = glucoseMeasurementRepository.save(measurement);
        alertService.generateForMeasurement(saved);

        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        device.setUpdatedBy(device.getUniqueIdentifier());
        deviceRepository.save(device);

        return new IngestMeasurementResponseDto(saved.getId(), Boolean.TRUE.equals(saved.getIsValid()), saved.getInvalidReason());
    }

    private String validateHeader(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new ApiException("MISSING_DEVICE_HEADER", headerName + " header is required.", HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private DeviceEntity resolveDevice(String deviceHeaderValue) {
        try {
            Long deviceId = Long.parseLong(deviceHeaderValue);
            return deviceRepository.findByIdAndActiveTrue(deviceId)
                    .orElseGet(() -> deviceRepository.findByUniqueIdentifierIgnoreCaseAndActiveTrue(deviceHeaderValue)
                            .orElseThrow(() -> new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED)));
        } catch (NumberFormatException ignored) {
            // Header is not numeric id; continue with identifier lookup.
        }
        return deviceRepository.findByUniqueIdentifierIgnoreCaseAndActiveTrue(deviceHeaderValue)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED));
    }
}

