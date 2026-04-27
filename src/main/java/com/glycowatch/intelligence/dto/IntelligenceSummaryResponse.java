package com.glycowatch.intelligence.dto;

import com.glycowatch.intelligence.model.AssistantMood;

public record IntelligenceSummaryResponse(
        String summary,
        AssistantMood mood
) {
}
