package com.glycowatch.intelligence.service;

import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.model.AssistantMood;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntelligenceServiceImpl implements IntelligenceService {

    private static final String DISCLAIMER =
            "This analysis is informational and does not replace professional medical advice.";

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public IntelligenceSummaryResponse getSummary(String authenticatedEmail) {
        resolveActiveUser(authenticatedEmail);

        return IntelligenceSummaryResponse.builder()
                .riskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .trend(GlucoseTrend.UNKNOWN.name())
                .confidence(IntelligenceConfidence.LOW.name())
                .assistantMood(AssistantMood.INSUFFICIENT_DATA.name())
                .summary("There is not enough analyzed data yet to generate an intelligence summary.")
                .detectedFactors(List.of("Insufficient analyzed data"))
                .recommendations(List.of("Continue recording measurements to enable future analysis."))
                .disclaimer(DISCLAIMER)
                .generatedAt(Instant.now())
                .build();
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }
}
