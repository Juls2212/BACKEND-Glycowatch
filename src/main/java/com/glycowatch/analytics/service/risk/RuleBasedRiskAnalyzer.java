package com.glycowatch.analytics.service.risk;

import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.analytics.dto.RiskAnalysisResponseDto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedRiskAnalyzer implements RiskAnalyzer {

    private static final BigDecimal TREND_DELTA = new BigDecimal("10");

    @Override
    public RiskAnalysisResponseDto analyze(RiskAnalysisContext context) {
        List<GlucoseMeasurementEntity> recentDesc = context.recentMeasurementsDesc();
        if (recentDesc == null || recentDesc.isEmpty()) {
            return new RiskAnalysisResponseDto(
                    RiskAnalysisResponseDto.CurrentStatus.IN_RANGE,
                    RiskAnalysisResponseDto.RiskLevel.LOW,
                    RiskAnalysisResponseDto.Trend.STABLE,
                    "No recent valid measurements are available."
            );
        }

        GlucoseMeasurementEntity latest = recentDesc.getFirst();
        BigDecimal lowThreshold = context.profile().getHypoglycemiaThreshold();
        BigDecimal highThreshold = context.profile().getHyperglycemiaThreshold();

        RiskAnalysisResponseDto.CurrentStatus currentStatus = resolveCurrentStatus(latest.getGlucoseValue(), lowThreshold, highThreshold);
        RiskAnalysisResponseDto.Trend trend = resolveTrend(recentDesc);
        int outOfRangeCount = countOutOfRange(recentDesc, lowThreshold, highThreshold, 5);
        RiskAnalysisResponseDto.RiskLevel riskLevel = resolveRiskLevel(currentStatus, outOfRangeCount, context.recentAlertsCount());
        String message = buildMessage(currentStatus, riskLevel, trend, latest.getGlucoseValue().toPlainString());

        return new RiskAnalysisResponseDto(currentStatus, riskLevel, trend, message);
    }

    private RiskAnalysisResponseDto.CurrentStatus resolveCurrentStatus(
            BigDecimal latestValue,
            BigDecimal lowThreshold,
            BigDecimal highThreshold
    ) {
        if (latestValue.compareTo(lowThreshold) < 0) {
            return RiskAnalysisResponseDto.CurrentStatus.LOW;
        }
        if (latestValue.compareTo(highThreshold) > 0) {
            return RiskAnalysisResponseDto.CurrentStatus.HIGH;
        }
        return RiskAnalysisResponseDto.CurrentStatus.IN_RANGE;
    }

    private RiskAnalysisResponseDto.Trend resolveTrend(List<GlucoseMeasurementEntity> recentDesc) {
        List<GlucoseMeasurementEntity> points = recentDesc.stream()
                .limit(5)
                .sorted(Comparator.comparing(GlucoseMeasurementEntity::getMeasuredAt))
                .toList();
        if (points.size() < 2) {
            return RiskAnalysisResponseDto.Trend.STABLE;
        }

        BigDecimal first = points.getFirst().getGlucoseValue();
        BigDecimal last = points.getLast().getGlucoseValue();
        BigDecimal delta = last.subtract(first);

        if (delta.compareTo(TREND_DELTA) > 0) {
            return RiskAnalysisResponseDto.Trend.RISING;
        }
        if (delta.compareTo(TREND_DELTA.negate()) < 0) {
            return RiskAnalysisResponseDto.Trend.FALLING;
        }
        return RiskAnalysisResponseDto.Trend.STABLE;
    }

    private int countOutOfRange(
            List<GlucoseMeasurementEntity> recentDesc,
            BigDecimal lowThreshold,
            BigDecimal highThreshold,
            int maxItems
    ) {
        return (int) recentDesc.stream()
                .limit(maxItems)
                .filter(m -> m.getGlucoseValue().compareTo(lowThreshold) < 0 || m.getGlucoseValue().compareTo(highThreshold) > 0)
                .count();
    }

    private RiskAnalysisResponseDto.RiskLevel resolveRiskLevel(
            RiskAnalysisResponseDto.CurrentStatus status,
            int outOfRangeCount,
            long recentAlertsCount
    ) {
        if (status != RiskAnalysisResponseDto.CurrentStatus.IN_RANGE && (outOfRangeCount >= 3 || recentAlertsCount >= 2)) {
            return RiskAnalysisResponseDto.RiskLevel.HIGH;
        }
        if (status != RiskAnalysisResponseDto.CurrentStatus.IN_RANGE || outOfRangeCount >= 2 || recentAlertsCount >= 1) {
            return RiskAnalysisResponseDto.RiskLevel.MEDIUM;
        }
        return RiskAnalysisResponseDto.RiskLevel.LOW;
    }

    private String buildMessage(
            RiskAnalysisResponseDto.CurrentStatus status,
            RiskAnalysisResponseDto.RiskLevel riskLevel,
            RiskAnalysisResponseDto.Trend trend,
            String latestValue
    ) {
        return "Current status is " + status + " with " + riskLevel + " risk. Trend is " + trend
                + ". Latest valid glucose is " + latestValue + " mg/dL.";
    }
}



