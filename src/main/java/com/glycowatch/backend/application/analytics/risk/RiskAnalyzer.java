package com.glycowatch.backend.application.analytics.risk;

import com.glycowatch.backend.interfaces.dto.analytics.RiskAnalysisResponseDto;

public interface RiskAnalyzer {

    RiskAnalysisResponseDto analyze(RiskAnalysisContext context);
}

