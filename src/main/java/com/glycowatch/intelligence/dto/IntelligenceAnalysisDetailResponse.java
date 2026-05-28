package com.glycowatch.intelligence.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceAnalysisDetailResponse {

    private Long id;
    private Instant generatedAt;
    private String ruleBasedRiskLevel;
    private String externalAiRiskLevel;
    private String finalRiskLevel;
    private String agreementStatus;
    private String trend;
    private String confidence;
    private String assistantMood;
    private String summary;
    private String aiExplanation;
    private String assistantMessage;
    private List<String> detectedFactors;
    private List<String> recommendations;
    private BigDecimal hypoglycemiaThreshold;
    private BigDecimal hyperglycemiaThreshold;
    private Map<String, Object> metrics;
    private List<Map<String, Object>> measurements;
    private Map<String, Object> ruleBasedAnalysis;
    private Map<String, Object> externalAiAnalysis;
    private Map<String, Object> finalMergedAnalysis;
}
