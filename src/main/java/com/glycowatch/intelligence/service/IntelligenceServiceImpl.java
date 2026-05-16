package com.glycowatch.intelligence.service;

import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.intelligence.dto.IntelligenceHistoryItemResponse;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.integration.ExternalAIAnalysisResult;
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

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final IntelligenceAnalysisRepository intelligenceAnalysisRepository;
    private final RuleBasedIntelligenceAnalyzer ruleBasedIntelligenceAnalyzer;
    private final IntelligenceSummaryMapper intelligenceSummaryMapper;
    private final ExternalIntelligenceProvider externalIntelligenceProvider;
    private final IntelligenceAnalysisPersistenceService intelligenceAnalysisPersistenceService;
    private final HybridRiskResolver hybridRiskResolver;

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
        String ruleBasedSummary = ruleBasedAnalysis.summary();
        HybridAnalysis hybridAnalysis = mergeExternalAIAnalysis(
                ruleBasedAnalysis.metrics(),
                trend,
                riskLevel,
                confidence,
                detectedFactors,
                recommendations,
                ruleBasedSummary
        );
        AssistantMood assistantMood = ruleBasedIntelligenceAnalyzer.determineAssistantMood(hybridAnalysis.finalRiskLevel());
        Instant generatedAt = Instant.now();

        IntelligenceSummaryResponse response = intelligenceSummaryMapper.toSummaryResponse(
                riskLevel,
                trend,
                confidence,
                assistantMood,
                hybridAnalysis.summary(),
                hybridAnalysis.externalAiRiskLevel(),
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

        intelligenceAnalysisPersistenceService.saveAnalysis(user.getId(), response);
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
        String summary = "Todavia no hay suficientes datos analizados para generar un resumen inteligente.";
        return intelligenceSummaryMapper.toInsufficientDataResponse(
                summary,
                "Todavia no cuento con suficiente informacion para darte un analisis mas preciso.",
                ruleBasedIntelligenceAnalyzer.determineAssistantMood(RiskLevel.INSUFFICIENT_DATA),
                List.of("Datos insuficientes para analizar tendencias recientes"),
                List.of("Continua registrando mediciones para habilitar un analisis mas completo."),
                DISCLAIMER,
                Instant.now()
        );
    }

    private String buildAssistantMessage(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "Continua con el monitoreo mientras se recopilan mas datos.";
        }

        return switch (riskLevel) {
            case LOW -> "Tus datos recientes se ven relativamente estables. Continua con tu seguimiento habitual.";
            case MODERATE -> "Detecte algunos cambios que merecen atencion. Conviene seguir tu glucosa de cerca.";
            case HIGH -> "Veo senales recientes que requieren mas atencion. Seria buena idea volver a medir pronto.";
            case CRITICAL -> "Detecte senales importantes en tus datos recientes. Revisa nuevamente tu glucosa y mantente atento a como te sientes.";
            case INSUFFICIENT_DATA -> "Todavia no hay suficientes datos para ofrecerte un analisis mas detallado.";
        };
    }

    private HybridAnalysis mergeExternalAIAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            IntelligenceConfidence confidence,
            List<String> detectedFactors,
            List<String> currentRecommendations,
            String summary
    ) {
        if (!externalIntelligenceProvider.isAvailable()) {
            return buildExternalAIUnavailable(ruleBasedRiskLevel, confidence, currentRecommendations, summary);
        }

        java.util.Optional<ExternalAIAnalysisResult> externalAiResultOptional = externalIntelligenceProvider.generateGlucoseAnalysis(
                metrics,
                trend,
                ruleBasedRiskLevel,
                detectedFactors,
                currentRecommendations
        );

        if (externalAiResultOptional.isEmpty()) {
            return buildExternalAIUnavailable(ruleBasedRiskLevel, confidence, currentRecommendations, summary);
        }

        ExternalAIAnalysisResult externalAiResult = externalAiResultOptional.get();
        RiskLevel externalAiRiskLevel;
        try {
            externalAiRiskLevel = RiskLevel.valueOf(externalAiResult.getRiskLevel().trim());
        } catch (IllegalArgumentException ex) {
            return buildExternalAIUnavailable(ruleBasedRiskLevel, confidence, currentRecommendations, summary);
        }

        HybridRiskResolver.Resolution resolution = hybridRiskResolver.resolve(
                ruleBasedRiskLevel,
                externalAiRiskLevel,
                confidence
        );
        List<String> finalRecommendations = externalAiResult.getRecommendations() != null && !externalAiResult.getRecommendations().isEmpty()
                ? externalAiResult.getRecommendations()
                : currentRecommendations;
        String aiExplanation = reinforceSafetyInExplanation(
                externalAiResult.getExplanation(),
                ruleBasedRiskLevel,
                externalAiRiskLevel
        );
        String assistantMessage = reinforceSafetyInAssistantMessage(
                externalAiResult.getAssistantMessage(),
                ruleBasedRiskLevel,
                externalAiRiskLevel
        );

        return new HybridAnalysis(
                externalAiRiskLevel.name(),
                resolution.finalRiskLevel(),
                resolution.agreementStatus(),
                resolution.confidence(),
                aiExplanation,
                aiExplanation,
                assistantMessage,
                Boolean.TRUE,
                finalRecommendations
        );
    }

    private HybridAnalysis buildExternalAIUnavailable(
            RiskLevel ruleBasedRiskLevel,
            IntelligenceConfidence confidence,
            List<String> currentRecommendations,
            String summary
    ) {
        HybridRiskResolver.Resolution resolution = hybridRiskResolver.resolve(ruleBasedRiskLevel, null, confidence);
        return new HybridAnalysis(
                null,
                resolution.finalRiskLevel(),
                resolution.agreementStatus(),
                resolution.confidence(),
                summary,
                summary,
                buildAssistantMessage(ruleBasedRiskLevel),
                Boolean.FALSE,
                currentRecommendations
        );
    }

    private String reinforceSafetyInExplanation(
            String aiExplanation,
            RiskLevel ruleBasedRiskLevel,
            RiskLevel externalAiRiskLevel
    ) {
        if (aiExplanation == null || !needsSafetyReinforcement(ruleBasedRiskLevel, externalAiRiskLevel)) {
            return aiExplanation;
        }

        return aiExplanation + switch (ruleBasedRiskLevel) {
            case CRITICAL ->
                    " Por seguridad, tus datos recientes siguen mostrando senales de riesgo critico y requieren vigilancia muy estrecha.";
            case HIGH ->
                    " Por seguridad, tus datos recientes mantienen senales de riesgo alto y conviene un seguimiento cercano.";
            default -> "";
        };
    }

    private String reinforceSafetyInAssistantMessage(
            String assistantMessage,
            RiskLevel ruleBasedRiskLevel,
            RiskLevel externalAiRiskLevel
    ) {
        if (assistantMessage == null || !needsSafetyReinforcement(ruleBasedRiskLevel, externalAiRiskLevel)) {
            return assistantMessage;
        }

        return assistantMessage + switch (ruleBasedRiskLevel) {
            case CRITICAL ->
                    " Como medida de seguridad, vuelve a medir pronto y mantente muy atento a como te sientes.";
            case HIGH ->
                    " Como medida de seguridad, conviene repetir la medicion pronto y observar la tendencia.";
            default -> "";
        };
    }

    private boolean needsSafetyReinforcement(RiskLevel ruleBasedRiskLevel, RiskLevel externalAiRiskLevel) {
        if (ruleBasedRiskLevel == null || externalAiRiskLevel == null) {
            return false;
        }

        return localRiskSeverity(ruleBasedRiskLevel) > localRiskSeverity(externalAiRiskLevel)
                && localRiskSeverity(ruleBasedRiskLevel) >= localRiskSeverity(RiskLevel.HIGH);
    }

    private int localRiskSeverity(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MODERATE -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case INSUFFICIENT_DATA -> 0;
        };
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

    private record ThresholdWindow(
            BigDecimal hypoglycemiaThreshold,
            BigDecimal hyperglycemiaThreshold
    ) {
    }

    private record HybridAnalysis(
            String externalAiRiskLevel,
            RiskLevel finalRiskLevel,
            AgreementStatus agreementStatus,
            IntelligenceConfidence confidence,
            String summary,
            String aiExplanation,
            String assistantMessage,
            Boolean geminiAvailable,
            List<String> recommendations
    ) {
    }
}
