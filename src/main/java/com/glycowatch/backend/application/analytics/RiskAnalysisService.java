package com.glycowatch.backend.application.analytics;

import com.glycowatch.backend.interfaces.dto.analytics.RiskAnalysisResponseDto;

public interface RiskAnalysisService {

    RiskAnalysisResponseDto getRiskAnalysis(String authenticatedEmail);
}

//risk