package com.glycowatch.analytics.service.risk;

import com.glycowatch.analytics.dto.RiskAnalysisResponseDto;

// Interface for analyzing risk based on user data and measurements, providing a structured response for risk analysis
public interface RiskAnalyzer {

    RiskAnalysisResponseDto analyze(RiskAnalysisContext context);
}



