package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;

public interface IntelligenceService {

    IntelligenceSummaryResponse getSummary(String authenticatedEmail);
}
