package com.glycowatch.intelligence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.model.IntelligenceAnalysis;
import com.glycowatch.intelligence.repository.IntelligenceAnalysisRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntelligenceAnalysisPersistenceService {

    private static final long DUPLICATE_WINDOW_MINUTES = 10;

    private final IntelligenceAnalysisRepository intelligenceAnalysisRepository;
    private final ObjectMapper objectMapper;

    public void saveAnalysis(Long userId, IntelligenceSummaryResponse response, IntelligenceAnalysisRecordContext context) {
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
                .hypoglycemiaThreshold(context == null ? null : context.hypoglycemiaThreshold())
                .hyperglycemiaThreshold(context == null ? null : context.hyperglycemiaThreshold())
                .metricsSnapshot(toJsonObject(context == null ? null : context.metricsSnapshot()))
                .measurementsSnapshot(toJsonObject(context == null ? null : context.measurementsSnapshot()))
                .ruleBasedAnalysisSnapshot(toJsonObject(context == null ? null : context.ruleBasedAnalysisSnapshot()))
                .externalAiAnalysisSnapshot(toJsonObject(context == null ? null : context.externalAiAnalysisSnapshot()))
                .finalMergedAnalysisSnapshot(toJsonObject(context == null ? null : context.finalMergedAnalysisSnapshot()))
                .createdAt(response.getGeneratedAt())
                .build();

        intelligenceAnalysisRepository.save(analysis);
    }

    public Optional<IntelligenceAnalysis> findLatestAnalysis(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return intelligenceAnalysisRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<IntelligenceAnalysis> findAnalysisDetail(Long userId, Long analysisId) {
        if (userId == null || analysisId == null) {
            return Optional.empty();
        }
        return intelligenceAnalysisRepository.findByIdAndUserId(analysisId, userId);
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

    private String toJsonObject(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    "INTELLIGENCE_SERIALIZATION_ERROR",
                    "Unable to serialize intelligence analysis details.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
