package com.glycowatch.backend.interfaces.dto.analytics;

public record RiskAnalysisResponseDto(
        CurrentStatus currentStatus,
        RiskLevel riskLevel,
        Trend trend,
        String message
) {
    public enum CurrentStatus {
        LOW,
        IN_RANGE,
        HIGH
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum Trend {
        STABLE,
        RISING,
        FALLING
    }
}

