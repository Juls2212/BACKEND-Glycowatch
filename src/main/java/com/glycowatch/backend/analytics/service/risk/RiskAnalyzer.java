package com.glycowatch.backend.analytics.service.risk;

import com.glycowatch.backend.analytics.dto.RiskAnalysisResponseDto;

public interface RiskAnalyzer {

    RiskAnalysisResponseDto analyze(RiskAnalysisContext context);
}


