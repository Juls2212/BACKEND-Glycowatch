package com.glycowatch.backend.analytics.service;

import com.glycowatch.backend.analytics.service.risk.RiskAnalysisContext;
import com.glycowatch.backend.analytics.service.risk.RiskAnalyzer;
import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.auth.model.UserEntity;
import com.glycowatch.backend.profile.model.UserProfileEntity;
import com.glycowatch.backend.alert.repository.AlertRepository;
import com.glycowatch.backend.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.profile.repository.UserProfileRepository;
import com.glycowatch.backend.auth.repository.UserRepository;
import com.glycowatch.backend.analytics.dto.RiskAnalysisResponseDto;
import com.glycowatch.backend.common.exception.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RiskAnalysisServiceImpl implements RiskAnalysisService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final AlertRepository alertRepository;
    private final RiskAnalyzer riskAnalyzer;

    @Override
    @Transactional(readOnly = true)
    public RiskAnalysisResponseDto getRiskAnalysis(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("PROFILE_NOT_FOUND", "User profile was not found.", HttpStatus.NOT_FOUND));

        List<GlucoseMeasurementEntity> recentMeasurements = glucoseMeasurementRepository
                .findTop20ByUserIdAndIsValidTrueOrderByMeasuredAtDesc(user.getId());

        long recentAlertsCount = alertRepository.countByUserIdAndCreatedAtGreaterThanEqual(
                user.getId(),
                Instant.now().minus(24, ChronoUnit.HOURS)
        );

        RiskAnalysisContext context = new RiskAnalysisContext(profile, recentMeasurements, recentAlertsCount);
        return riskAnalyzer.analyze(context);
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }
}


