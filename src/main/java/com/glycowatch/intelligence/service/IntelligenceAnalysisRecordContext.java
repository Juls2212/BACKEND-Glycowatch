package com.glycowatch.intelligence.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record IntelligenceAnalysisRecordContext(
        BigDecimal hypoglycemiaThreshold,
        BigDecimal hyperglycemiaThreshold,
        Map<String, Object> metricsSnapshot,
        List<Map<String, Object>> measurementsSnapshot,
        Map<String, Object> ruleBasedAnalysisSnapshot,
        Map<String, Object> externalAiAnalysisSnapshot,
        Map<String, Object> finalMergedAnalysisSnapshot,
        List<String> detectedFactorsSnapshot,
        List<String> recommendationsSnapshot
) {
}
