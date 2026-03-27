package com.glycowatch.backend.application.analytics;

import com.glycowatch.backend.application.analytics.risk.RiskAnalysisContext;
import com.glycowatch.backend.application.analytics.risk.RiskAnalyzer;
import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.domain.user.UserEntity;
import com.glycowatch.backend.domain.user.UserProfileEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.AlertRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserProfileRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserRepository;
import com.glycowatch.backend.interfaces.dto.analytics.RiskAnalysisResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
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

