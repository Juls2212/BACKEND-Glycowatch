package com.glycowatch.backend.analytics.service;

import com.glycowatch.backend.analytics.dto.RiskAnalysisResponseDto;

public interface RiskAnalysisService {

    RiskAnalysisResponseDto getRiskAnalysis(String authenticatedEmail);
}


