package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.integration.ExternalAIAnalysisResult;
import com.glycowatch.intelligence.integration.GeminiClient;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.RiskLevel;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeminiIntelligenceProvider implements ExternalIntelligenceProvider {

    private final GeminiClient geminiClient;

    @Override
    public boolean isAvailable() {
        return geminiClient.isAvailable();
    }

    @Override
    public Optional<ExternalAIAnalysisResult> generateGlucoseAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    ) {
        return geminiClient.generateGlucoseAnalysis(
                metrics,
                trend,
                ruleBasedRiskLevel,
                detectedFactors,
                currentRecommendations
        );
    }
}
