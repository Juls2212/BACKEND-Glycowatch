package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.dto.IntelligenceHistoryItemResponse;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import java.util.List;

public interface IntelligenceService {

    IntelligenceSummaryResponse getSummary(String authenticatedEmail);

    List<IntelligenceHistoryItemResponse> getHistory(String authenticatedEmail);
}
