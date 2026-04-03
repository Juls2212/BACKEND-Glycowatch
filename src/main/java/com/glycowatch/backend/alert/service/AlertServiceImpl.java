package com.glycowatch.backend.alert.service;

import com.glycowatch.backend.alert.model.AlertEntity;
import com.glycowatch.backend.alert.model.AlertType;
import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.auth.model.UserEntity;
import com.glycowatch.backend.profile.model.UserProfileEntity;
import com.glycowatch.backend.alert.repository.AlertRepository;
import com.glycowatch.backend.profile.repository.UserProfileRepository;
import com.glycowatch.backend.auth.repository.UserRepository;
import com.glycowatch.backend.alert.dto.AlertResponseDto;
import com.glycowatch.backend.common.exception.ApiException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void generateForMeasurement(GlucoseMeasurementEntity measurement) {
        if (!Boolean.TRUE.equals(measurement.getIsValid())) {
            return;
        }

        UserProfileEntity profile = userProfileRepository.findByUserId(measurement.getUser().getId())
                .orElseThrow(() -> new ApiException("PROFILE_NOT_FOUND", "User profile was not found.", HttpStatus.NOT_FOUND));

        AlertType type = resolveAlertType(measurement.getGlucoseValue().doubleValue(),
                profile.getHypoglycemiaThreshold().doubleValue(),
                profile.getHyperglycemiaThreshold().doubleValue());

        if (type == null) {
            return;
        }

        AlertEntity alert = AlertEntity.builder()
                .user(measurement.getUser())
                .measurement(measurement)
                .type(type)
                .message(buildMessage(type, measurement.getGlucoseValue().toPlainString()))
                .isRead(Boolean.FALSE)
                .createdAt(Instant.now())
                .createdBy("SYSTEM")
                .build();
        alertRepository.save(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDto> getAlerts(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        return alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toAlertResponse)
                .toList();
    }

    @Override
    @Transactional
    public AlertResponseDto markAsRead(String authenticatedEmail, Long alertId) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        AlertEntity alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new ApiException("ALERT_NOT_FOUND", "Alert was not found.", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(alert.getIsRead())) {
            alert.setIsRead(Boolean.TRUE);
            alert.setReadAt(Instant.now());
            alert.setUpdatedAt(Instant.now());
            alert.setUpdatedBy(user.getEmail());
            alert = alertRepository.save(alert);
        }

        return toAlertResponse(alert);
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private AlertType resolveAlertType(double value, double lowThreshold, double highThreshold) {
        if (value < lowThreshold) {
            return AlertType.LOW_GLUCOSE;
        }
        if (value > highThreshold) {
            return AlertType.HIGH_GLUCOSE;
        }
        return null;
    }

    private String buildMessage(AlertType type, String glucoseValue) {
        if (type == AlertType.HIGH_GLUCOSE) {
            return "High glucose detected: " + glucoseValue + " mg/dL.";
        }
        return "Low glucose detected: " + glucoseValue + " mg/dL.";
    }

    private AlertResponseDto toAlertResponse(AlertEntity alert) {
        return new AlertResponseDto(
                alert.getId(),
                alert.getType(),
                alert.getMessage(),
                Boolean.TRUE.equals(alert.getIsRead()),
                alert.getReadAt(),
                alert.getMeasurement().getId(),
                alert.getCreatedAt()
        );
    }
}


