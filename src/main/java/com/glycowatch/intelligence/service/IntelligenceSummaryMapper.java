package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.dto.IntelligenceAnalysisDetailResponse;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.model.IntelligenceAnalysis;
import com.glycowatch.intelligence.model.AgreementStatus;
import com.glycowatch.intelligence.model.AssistantMood;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IntelligenceSummaryMapper {

    public IntelligenceSummaryResponse toSummaryResponse(
            RiskLevel ruleBasedRiskLevel,
            GlucoseTrend trend,
            IntelligenceConfidence confidence,
            AssistantMood assistantMood,
            String summary,
            String geminiRiskLevel,
            RiskLevel finalRiskLevel,
            AgreementStatus agreementStatus,
            String aiExplanation,
            String assistantMessage,
            Boolean geminiAvailable,
            List<String> detectedFactors,
            List<String> recommendations,
            String disclaimer,
            Instant generatedAt
    ) {
        return IntelligenceSummaryResponse.builder()
                .riskLevel(ruleBasedRiskLevel.name())
                .ruleBasedRiskLevel(ruleBasedRiskLevel.name())
                .geminiRiskLevel(geminiRiskLevel)
                .finalRiskLevel(finalRiskLevel.name())
                .agreementStatus(agreementStatus.name())
                .trend(trend.name())
                .confidence(confidence.name())
                .assistantMood(assistantMood.name())
                .summary(summary)
                .aiExplanation(aiExplanation)
                .assistantMessage(assistantMessage)
                .geminiAvailable(geminiAvailable)
                .detectedFactors(detectedFactors)
                .recommendations(recommendations)
                .disclaimer(disclaimer)
                .generatedAt(generatedAt)
                .build();
    }

    public IntelligenceSummaryResponse toInsufficientDataResponse(
            String summary,
            String assistantMessage,
            AssistantMood assistantMood,
            List<String> detectedFactors,
            List<String> recommendations,
            String disclaimer,
            Instant generatedAt
    ) {
        return IntelligenceSummaryResponse.builder()
                .riskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .ruleBasedRiskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .geminiRiskLevel(null)
                .finalRiskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .agreementStatus(AgreementStatus.GEMINI_UNAVAILABLE.name())
                .trend(GlucoseTrend.UNKNOWN.name())
                .confidence(IntelligenceConfidence.LOW.name())
                .assistantMood(assistantMood.name())
                .summary(summary)
                .aiExplanation(summary)
                .assistantMessage(assistantMessage)
                .geminiAvailable(Boolean.FALSE)
                .detectedFactors(detectedFactors)
                .recommendations(recommendations)
                .disclaimer(disclaimer)
                .generatedAt(generatedAt)
                .build();
    }

    public IntelligenceSummaryResponse fromStoredAnalysis(
            IntelligenceAnalysis analysis,
            List<String> detectedFactors,
            List<String> recommendations,
            String disclaimer
    ) {
        return IntelligenceSummaryResponse.builder()
                .riskLevel(analysis.getRuleBasedRiskLevel())
                .ruleBasedRiskLevel(analysis.getRuleBasedRiskLevel())
                .geminiRiskLevel(analysis.getGeminiRiskLevel())
                .finalRiskLevel(analysis.getFinalRiskLevel())
                .agreementStatus(analysis.getAgreementStatus())
                .trend(analysis.getTrend())
                .confidence(analysis.getConfidence())
                .assistantMood(analysis.getAssistantMood())
                .summary(analysis.getSummary())
                .aiExplanation(analysis.getAiExplanation())
                .assistantMessage(analysis.getAssistantMessage())
                .geminiAvailable(analysis.getGeminiRiskLevel() != null)
                .detectedFactors(detectedFactors)
                .recommendations(recommendations)
                .disclaimer(disclaimer)
                .generatedAt(analysis.getCreatedAt())
                .build();
    }

    public IntelligenceAnalysisDetailResponse toDetailResponse(
            IntelligenceAnalysis analysis,
            List<String> detectedFactors,
            List<String> recommendations,
            Map<String, Object> metrics,
            List<Map<String, Object>> measurements,
            Map<String, Object> ruleBasedAnalysis,
            Map<String, Object> externalAiAnalysis,
            Map<String, Object> finalMergedAnalysis
    ) {
        return IntelligenceAnalysisDetailResponse.builder()
                .id(analysis.getId())
                .generatedAt(analysis.getCreatedAt())
                .ruleBasedRiskLevel(analysis.getRuleBasedRiskLevel())
                .externalAiRiskLevel(analysis.getGeminiRiskLevel())
                .finalRiskLevel(analysis.getFinalRiskLevel())
                .agreementStatus(analysis.getAgreementStatus())
                .trend(analysis.getTrend())
                .confidence(analysis.getConfidence())
                .assistantMood(analysis.getAssistantMood())
                .summary(analysis.getSummary())
                .aiExplanation(analysis.getAiExplanation())
                .assistantMessage(analysis.getAssistantMessage())
                .detectedFactors(detectedFactors)
                .recommendations(recommendations)
                .hypoglycemiaThreshold(analysis.getHypoglycemiaThreshold())
                .hyperglycemiaThreshold(analysis.getHyperglycemiaThreshold())
                .metrics(metrics)
                .measurements(measurements)
                .ruleBasedAnalysis(ruleBasedAnalysis)
                .externalAiAnalysis(externalAiAnalysis)
                .finalMergedAnalysis(finalMergedAnalysis)
                .build();
    }
}
