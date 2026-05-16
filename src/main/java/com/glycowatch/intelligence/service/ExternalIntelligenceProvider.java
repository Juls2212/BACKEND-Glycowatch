package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.integration.ExternalAIAnalysisResult;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.RiskLevel;
import java.util.List;
import java.util.Optional;

public interface ExternalIntelligenceProvider {

    boolean isAvailable();

    Optional<ExternalAIAnalysisResult> generateGlucoseAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    );
}
