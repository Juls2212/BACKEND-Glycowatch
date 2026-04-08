package com.glycowatch.analytics.service;

import com.glycowatch.analytics.dto.RiskAnalysisResponseDto;

public interface RiskAnalysisService {

    RiskAnalysisResponseDto getRiskAnalysis(String authenticatedEmail);
}



