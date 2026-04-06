package com.glycowatch.backend.analytics.service.risk;

import com.glycowatch.backend.analytics.dto.RiskAnalysisResponseDto;

// Interface for analyzing risk based on user data and measurements, providing a structured response for risk analysis
public interface RiskAnalyzer {

    RiskAnalysisResponseDto analyze(RiskAnalysisContext context);
}


