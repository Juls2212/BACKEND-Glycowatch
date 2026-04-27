package com.glycowatch.intelligence.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceSummaryResponse {

    private String riskLevel;
    private String trend;
    private String confidence;
    private String assistantMood;
    private String summary;
    private List<String> detectedFactors;
    private List<String> recommendations;
    private String disclaimer;
    private Instant generatedAt;
}
