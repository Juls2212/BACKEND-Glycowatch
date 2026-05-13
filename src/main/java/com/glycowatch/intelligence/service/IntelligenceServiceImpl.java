package com.glycowatch.intelligence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.intelligence.dto.IntelligenceHistoryItemResponse;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.integration.GeminiAnalysisResult;
import com.glycowatch.intelligence.model.AgreementStatus;
import com.glycowatch.intelligence.model.IntelligenceAnalysis;
import com.glycowatch.intelligence.model.AssistantMood;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import com.glycowatch.intelligence.repository.IntelligenceAnalysisRepository;
import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.profile.model.UserProfileEntity;
import com.glycowatch.profile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntelligenceServiceImpl implements IntelligenceService {

    private static final String DISCLAIMER =
            "This analysis is informational and does not replace professional medical advice.";
    private static final BigDecimal DEFAULT_HYPOGLYCEMIA_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal DEFAULT_HYPERGLYCEMIA_THRESHOLD = new BigDecimal("180");
    private static final long DUPLICATE_WINDOW_MINUTES = 10;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final IntelligenceAnalysisRepository intelligenceAnalysisRepository;
    private final RuleBasedIntelligenceAnalyzer ruleBasedIntelligenceAnalyzer;
    private final IntelligenceSummaryMapper intelligenceSummaryMapper;
    private final ExternalIntelligenceProvider externalIntelligenceProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public IntelligenceSummaryResponse getSummary(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        ThresholdWindow thresholds = resolveThresholds(profile);
        List<GlucoseMeasurementEntity> last7DaysMeasurements = getMeasurementsLast7Days(user.getId());
        List<GlucoseMeasurementEntity> last24HoursMeasurements = getMeasurementsLast24Hours(user.getId());
        RuleBasedIntelligenceAnalyzer.RuleBasedAnalysis ruleBasedAnalysis = ruleBasedIntelligenceAnalyzer.analyze(
                last24HoursMeasurements,
                last7DaysMeasurements,
                thresholds.hypoglycemiaThreshold(),
                thresholds.hyperglycemiaThreshold()
        );

        consumePreparedInputs(user, thresholds, last7DaysMeasurements, last24HoursMeasurements, ruleBasedAnalysis);

        if (!ruleBasedAnalysis.hasSufficientData()) {
            return buildInsufficientDataResponse();
        }

        GlucoseTrend trend = ruleBasedAnalysis.trend();
        RiskLevel riskLevel = ruleBasedAnalysis.riskLevel();
        List<String> detectedFactors = ruleBasedAnalysis.detectedFactors();
        List<String> recommendations = ruleBasedAnalysis.recommendations();
        IntelligenceConfidence confidence = ruleBasedAnalysis.confidence();
        String summary = ruleBasedAnalysis.summary();
        HybridAnalysis hybridAnalysis = mergeGeminiAnalysis(
                ruleBasedAnalysis.metrics(),
                trend,
                riskLevel,
                detectedFactors,
                recommendations,
                summary
        );
        AssistantMood assistantMood = ruleBasedIntelligenceAnalyzer.determineAssistantMood(hybridAnalysis.finalRiskLevel());
        Instant generatedAt = Instant.now();

        IntelligenceSummaryResponse response = intelligenceSummaryMapper.toSummaryResponse(
                riskLevel,
                trend,
                confidence,
                assistantMood,
                summary,
                hybridAnalysis.geminiRiskLevel(),
                hybridAnalysis.finalRiskLevel(),
                hybridAnalysis.agreementStatus(),
                hybridAnalysis.aiExplanation(),
                hybridAnalysis.assistantMessage(),
                hybridAnalysis.geminiAvailable(),
                detectedFactors,
                hybridAnalysis.recommendations(),
                DISCLAIMER,
                generatedAt
        );

        saveAnalysis(user.getId(), response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntelligenceHistoryItemResponse> getHistory(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        return intelligenceAnalysisRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toHistoryItemResponse)
                .toList();
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private ThresholdWindow resolveThresholds(UserProfileEntity profile) {
        if (profile == null) {
            return new ThresholdWindow(DEFAULT_HYPOGLYCEMIA_THRESHOLD, DEFAULT_HYPERGLYCEMIA_THRESHOLD);
        }

        BigDecimal hypoglycemiaThreshold = profile.getHypoglycemiaThreshold() != null
                ? profile.getHypoglycemiaThreshold()
                : DEFAULT_HYPOGLYCEMIA_THRESHOLD;
        BigDecimal hyperglycemiaThreshold = profile.getHyperglycemiaThreshold() != null
                ? profile.getHyperglycemiaThreshold()
                : DEFAULT_HYPERGLYCEMIA_THRESHOLD;

        return new ThresholdWindow(hypoglycemiaThreshold, hyperglycemiaThreshold);
    }

    private List<GlucoseMeasurementEntity> getMeasurementsLast7Days(Long userId) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(userId, since);
    }

    private List<GlucoseMeasurementEntity> getMeasurementsLast24Hours(Long userId) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(userId, since);
    }

    private void consumePreparedInputs(
            UserEntity user,
            ThresholdWindow thresholds,
            List<GlucoseMeasurementEntity> last7DaysMeasurements,
            List<GlucoseMeasurementEntity> last24HoursMeasurements,
            RuleBasedIntelligenceAnalyzer.RuleBasedAnalysis ruleBasedAnalysis
    ) {
        // Intentionally unused for now; this keeps the service prepared for the next real analysis step.
        if (user == null
                || thresholds == null
                || last7DaysMeasurements == null
                || last24HoursMeasurements == null
                || ruleBasedAnalysis == null
                || ruleBasedAnalysis.metrics() == null) {
            throw new IllegalStateException("Prepared intelligence inputs must not be null.");
        }
    }

    private IntelligenceSummaryResponse buildInsufficientDataResponse() {
        String summary = "There is not enough analyzed data yet to generate an intelligence summary.";
        return intelligenceSummaryMapper.toInsufficientDataResponse(
                summary,
                "Not enough data is available yet to provide a more detailed analysis.",
                ruleBasedIntelligenceAnalyzer.determineAssistantMood(RiskLevel.INSUFFICIENT_DATA),
                List.of("Insufficient analyzed data"),
                List.of("Continue recording measurements to enable future analysis."),
                DISCLAIMER,
                Instant.now()
        );
    }

    private String buildAssistantMessage(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "Continue consistent monitoring while more data is collected.";
        }

        return switch (riskLevel) {
            case LOW -> "Current data looks relatively stable. Continue consistent monitoring.";
            case MODERATE -> "Some changes were detected. Keep monitoring your glucose closely.";
            case HIGH -> "Recent data shows elevated attention signals. Consider checking your glucose again soon.";
            case CRITICAL -> "Recent data shows strong warning signals. Recheck your glucose and stay attentive to how you feel.";
            case INSUFFICIENT_DATA -> "Not enough data is available yet to provide a more detailed analysis.";
        };
    }

    private HybridAnalysis mergeGeminiAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations,
            String summary
    ) {
        if (!externalIntelligenceProvider.isAvailable()) {
            return buildGeminiUnavailable(ruleBasedRiskLevel, currentRecommendations, summary);
        }

        java.util.Optional<GeminiAnalysisResult> geminiResultOptional = externalIntelligenceProvider.generateGlucoseAnalysis(
                metrics,
                trend,
                ruleBasedRiskLevel,
                detectedFactors,
                currentRecommendations
        );

        if (geminiResultOptional.isEmpty()) {
            return buildGeminiUnavailable(ruleBasedRiskLevel, currentRecommendations, summary);
        }

        GeminiAnalysisResult geminiResult = geminiResultOptional.get();
        RiskLevel geminiRiskLevel;
        try {
            geminiRiskLevel = RiskLevel.valueOf(geminiResult.getRiskLevel().trim());
        } catch (IllegalArgumentException ex) {
            return buildGeminiUnavailable(ruleBasedRiskLevel, currentRecommendations, summary);
        }

        RiskLevel finalRiskLevel = moreConservativeRisk(ruleBasedRiskLevel, geminiRiskLevel);
        AgreementStatus agreementStatus = determineAgreementStatus(ruleBasedRiskLevel, geminiRiskLevel);
        List<String> finalRecommendations = geminiResult.getRecommendations() != null && !geminiResult.getRecommendations().isEmpty()
                ? geminiResult.getRecommendations()
                : currentRecommendations;

        return new HybridAnalysis(
                geminiRiskLevel.name(),
                finalRiskLevel,
                agreementStatus,
                geminiResult.getExplanation(),
                geminiResult.getAssistantMessage(),
                Boolean.TRUE,
                finalRecommendations
        );
    }

    private HybridAnalysis buildGeminiUnavailable(
            RiskLevel ruleBasedRiskLevel,
            List<String> currentRecommendations,
            String summary
    ) {
        return new HybridAnalysis(
                null,
                ruleBasedRiskLevel,
                AgreementStatus.GEMINI_UNAVAILABLE,
                summary,
                buildAssistantMessage(ruleBasedRiskLevel),
                Boolean.FALSE,
                currentRecommendations
        );
    }

    private AgreementStatus determineAgreementStatus(RiskLevel ruleBasedRiskLevel, RiskLevel geminiRiskLevel) {
        if (ruleBasedRiskLevel == null || geminiRiskLevel == null) {
            return AgreementStatus.GEMINI_UNAVAILABLE;
        }
        if (ruleBasedRiskLevel == geminiRiskLevel) {
            return AgreementStatus.FULL_AGREEMENT;
        }

        int difference = Math.abs(riskSeverity(ruleBasedRiskLevel) - riskSeverity(geminiRiskLevel));
        return difference == 1 ? AgreementStatus.PARTIAL_AGREEMENT : AgreementStatus.DISAGREEMENT;
    }

    private RiskLevel moreConservativeRisk(RiskLevel left, RiskLevel right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return riskSeverity(left) >= riskSeverity(right) ? left : right;
    }

    private void saveAnalysis(Long userId, IntelligenceSummaryResponse response) {
        if (userId == null || response == null) {
            return;
        }

        if (shouldSkipSave(userId, response)) {
            return;
        }

        IntelligenceAnalysis analysis = IntelligenceAnalysis.builder()
                .userId(userId)
                .ruleBasedRiskLevel(response.getRuleBasedRiskLevel())
                .geminiRiskLevel(response.getGeminiRiskLevel())
                .finalRiskLevel(response.getFinalRiskLevel())
                .trend(response.getTrend())
                .confidence(response.getConfidence())
                .assistantMood(response.getAssistantMood())
                .summary(response.getSummary())
                .aiExplanation(response.getAiExplanation())
                .assistantMessage(response.getAssistantMessage())
                .detectedFactors(toJson(response.getDetectedFactors()))
                .recommendations(toJson(response.getRecommendations()))
                .agreementStatus(response.getAgreementStatus())
                .createdAt(response.getGeneratedAt())
                .build();

        intelligenceAnalysisRepository.save(analysis);
    }

    private boolean shouldSkipSave(Long userId, IntelligenceSummaryResponse response) {
        return intelligenceAnalysisRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(existing -> wasCreatedWithinDuplicateWindow(existing.getCreatedAt(), response.getGeneratedAt()))
                .filter(existing -> isEquivalentAnalysis(existing, response))
                .isPresent();
    }

    private boolean wasCreatedWithinDuplicateWindow(Instant existingCreatedAt, Instant newGeneratedAt) {
        if (existingCreatedAt == null || newGeneratedAt == null) {
            return false;
        }

        Instant duplicateThreshold = newGeneratedAt.minus(DUPLICATE_WINDOW_MINUTES, ChronoUnit.MINUTES);
        return !existingCreatedAt.isBefore(duplicateThreshold);
    }

    private boolean isEquivalentAnalysis(IntelligenceAnalysis existing, IntelligenceSummaryResponse response) {
        if (existing == null || response == null) {
            return false;
        }

        return Objects.equals(existing.getFinalRiskLevel(), response.getFinalRiskLevel())
                && Objects.equals(existing.getTrend(), response.getTrend())
                && Objects.equals(existing.getAssistantMood(), response.getAssistantMood())
                && Objects.equals(existing.getSummary(), response.getSummary())
                && Objects.equals(existing.getAssistantMessage(), response.getAssistantMessage())
                && Objects.equals(existing.getAgreementStatus(), response.getAgreementStatus());
    }

    private IntelligenceHistoryItemResponse toHistoryItemResponse(IntelligenceAnalysis analysis) {
        return IntelligenceHistoryItemResponse.builder()
                .id(analysis.getId())
                .finalRiskLevel(analysis.getFinalRiskLevel())
                .trend(analysis.getTrend())
                .assistantMood(analysis.getAssistantMood())
                .summary(analysis.getSummary())
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    private String toJson(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        try {
            return objectMapper.writeValueAsString(safeValues);
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    "INTELLIGENCE_SERIALIZATION_ERROR",
                    "Unable to serialize intelligence analysis details.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private int riskSeverity(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MODERATE -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case INSUFFICIENT_DATA -> 0;
        };
    }

    private record ThresholdWindow(
            BigDecimal hypoglycemiaThreshold,
            BigDecimal hyperglycemiaThreshold
    ) {
    }

    private record HybridAnalysis(
            String geminiRiskLevel,
            RiskLevel finalRiskLevel,
            AgreementStatus agreementStatus,
            String aiExplanation,
            String assistantMessage,
            Boolean geminiAvailable,
            List<String> recommendations
    ) {
    }
}
