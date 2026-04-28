package com.glycowatch.intelligence.dto;

import java.time.Instant;
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
public class IntelligenceHistoryItemResponse {

    private Long id;
    private String finalRiskLevel;
    private String trend;
    private String assistantMood;
    private String summary;
    private Instant createdAt;
}
