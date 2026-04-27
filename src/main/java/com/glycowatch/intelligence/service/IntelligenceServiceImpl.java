package com.glycowatch.intelligence.service;

import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.model.AgreementStatus;
import com.glycowatch.intelligence.model.AssistantMood;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.profile.model.UserProfileEntity;
import com.glycowatch.profile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntelligenceServiceImpl implements IntelligenceService {

    private static final String DISCLAIMER =
            "This analysis is informational and does not replace professional medical advice.";
    private static final BigDecimal DEFAULT_HYPOGLYCEMIA_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal DEFAULT_HYPERGLYCEMIA_THRESHOLD = new BigDecimal("180");
    private static final double TREND_DELTA_THRESHOLD = 15.0;
    private static final double HIGH_VARIABILITY_THRESHOLD = 40.0;
    private static final double RISK_VARIABILITY_THRESHOLD = 80.0;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;

    @Override
    @Transactional(readOnly = true)
    public IntelligenceSummaryResponse getSummary(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        ThresholdWindow thresholds = resolveThresholds(profile);
        List<GlucoseMeasurementEntity> last7DaysMeasurements = getMeasurementsLast7Days(user.getId());
        List<GlucoseMeasurementEntity> last24HoursMeasurements = getMeasurementsLast24Hours(user.getId());
        GlucoseAnalysisMetrics metrics = computeMetrics(
                last24HoursMeasurements,
                last7DaysMeasurements,
                thresholds.hypoglycemiaThreshold(),
                thresholds.hyperglycemiaThreshold()
        );

        consumePreparedInputs(user, thresholds, last7DaysMeasurements, last24HoursMeasurements, metrics);

        if (last7DaysMeasurements.size() < 3) {
            return buildInsufficientDataResponse();
        }

        GlucoseTrend trend = calculateTrend(last7DaysMeasurements);
        RiskLevel riskLevel = calculateRisk(
                metrics,
                trend,
                thresholds.hypoglycemiaThreshold().doubleValue(),
                thresholds.hyperglycemiaThreshold().doubleValue()
        );
        List<String> detectedFactors = generateFactors(
                metrics,
                trend,
                thresholds.hypoglycemiaThreshold().doubleValue(),
                thresholds.hyperglycemiaThreshold().doubleValue()
        );
        List<String> recommendations = generateRecommendations(
                metrics,
                trend,
                thresholds.hypoglycemiaThreshold().doubleValue(),
                thresholds.hyperglycemiaThreshold().doubleValue()
        );
        IntelligenceConfidence confidence = calculateConfidence(metrics.getCountLast7d());
        AssistantMood assistantMood = determineAssistantMood(riskLevel);
        String summary = buildSummary(riskLevel, trend, metrics);

        return IntelligenceSummaryResponse.builder()
                .riskLevel(riskLevel.name())
                .ruleBasedRiskLevel(riskLevel.name())
                .geminiRiskLevel(null)
                .finalRiskLevel(riskLevel.name())
                .agreementStatus(AgreementStatus.GEMINI_UNAVAILABLE.name())
                .trend(trend.name())
                .confidence(confidence.name())
                .assistantMood(assistantMood.name())
                .summary(summary)
                .aiExplanation(summary)
                .assistantMessage(buildAssistantMessage(riskLevel))
                .geminiAvailable(Boolean.FALSE)
                .detectedFactors(detectedFactors)
                .recommendations(recommendations)
                .disclaimer(DISCLAIMER)
                .generatedAt(Instant.now())
                .build();
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private ThresholdWindow resolveThresholds(UserProfileEntity profile) {
        if (profile == null) {
            return new ThresholdWindow(DEFAULT_HYPOGLYCEMIA_THRESHOLD, DEFAULT_HYPERGLYCEMIA_THRESHOLD);
        }

        BigDecimal hypoglycemiaThreshold = profile.getHypoglycemiaThreshold() != null
                ? profile.getHypoglycemiaThreshold()
                : DEFAULT_HYPOGLYCEMIA_THRESHOLD;
        BigDecimal hyperglycemiaThreshold = profile.getHyperglycemiaThreshold() != null
                ? profile.getHyperglycemiaThreshold()
                : DEFAULT_HYPERGLYCEMIA_THRESHOLD;

        return new ThresholdWindow(hypoglycemiaThreshold, hyperglycemiaThreshold);
    }

    private List<GlucoseMeasurementEntity> getMeasurementsLast7Days(Long userId) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(userId, since);
    }

    private List<GlucoseMeasurementEntity> getMeasurementsLast24Hours(Long userId) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(userId, since);
    }

    private void consumePreparedInputs(
            UserEntity user,
            ThresholdWindow thresholds,
            List<GlucoseMeasurementEntity> last7DaysMeasurements,
            List<GlucoseMeasurementEntity> last24HoursMeasurements,
            GlucoseAnalysisMetrics metrics
    ) {
        // Intentionally unused for now; this keeps the service prepared for the next real analysis step.
        if (user == null
                || thresholds == null
                || last7DaysMeasurements == null
                || last24HoursMeasurements == null
                || metrics == null) {
            throw new IllegalStateException("Prepared intelligence inputs must not be null.");
        }
    }

    private IntelligenceSummaryResponse buildInsufficientDataResponse() {
        String summary = "There is not enough analyzed data yet to generate an intelligence summary.";
        return IntelligenceSummaryResponse.builder()
                .riskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .ruleBasedRiskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .geminiRiskLevel(null)
                .finalRiskLevel(RiskLevel.INSUFFICIENT_DATA.name())
                .agreementStatus(AgreementStatus.GEMINI_UNAVAILABLE.name())
                .trend(GlucoseTrend.UNKNOWN.name())
                .confidence(IntelligenceConfidence.LOW.name())
                .assistantMood(AssistantMood.INSUFFICIENT_DATA.name())
                .summary(summary)
                .aiExplanation(summary)
                .assistantMessage("Not enough data is available yet to provide a more detailed analysis.")
                .geminiAvailable(Boolean.FALSE)
                .detectedFactors(List.of("Insufficient analyzed data"))
                .recommendations(List.of("Continue recording measurements to enable future analysis."))
                .disclaimer(DISCLAIMER)
                .generatedAt(Instant.now())
                .build();
    }

    private GlucoseAnalysisMetrics computeMetrics(
            List<GlucoseMeasurementEntity> last24HoursMeasurements,
            List<GlucoseMeasurementEntity> last7DaysMeasurements,
            BigDecimal lowThreshold,
            BigDecimal highThreshold
    ) {
        GlucoseAnalysisMetrics metrics = new GlucoseAnalysisMetrics();

        List<GlucoseMeasurementEntity> safeLast24HoursMeasurements =
                last24HoursMeasurements == null ? List.of() : last24HoursMeasurements;
        List<GlucoseMeasurementEntity> safeLast7DaysMeasurements =
                last7DaysMeasurements == null ? List.of() : last7DaysMeasurements;

        metrics.setCountLast24h(safeLast24HoursMeasurements.size());
        metrics.setCountLast7d(safeLast7DaysMeasurements.size());
        metrics.setAverageLast24h(averageOf(safeLast24HoursMeasurements));
        metrics.setAverageLast7d(averageOf(safeLast7DaysMeasurements));
        metrics.setMinLast7d(minOf(safeLast7DaysMeasurements));
        metrics.setMaxLast7d(maxOf(safeLast7DaysMeasurements));
        metrics.setLatestValue(latestValueOf(safeLast24HoursMeasurements, safeLast7DaysMeasurements));
        metrics.setLowReadingsCount(countBelowThreshold(safeLast7DaysMeasurements, lowThreshold));
        metrics.setHighReadingsCount(countAboveThreshold(safeLast7DaysMeasurements, highThreshold));

        Double minLast7d = metrics.getMinLast7d();
        Double maxLast7d = metrics.getMaxLast7d();
        metrics.setVariability(minLast7d == null || maxLast7d == null ? null : maxLast7d - minLast7d);

        return metrics;
    }

    private Double averageOf(List<GlucoseMeasurementEntity> measurements) {
        if (measurements.isEmpty()) {
            return null;
        }

        java.util.OptionalDouble average = measurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .filter(value -> value != null)
                .mapToDouble(BigDecimal::doubleValue)
                .average();

        return average.isPresent() ? average.getAsDouble() : null;
    }

    private Double minOf(List<GlucoseMeasurementEntity> measurements) {
        return measurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .filter(value -> value != null)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::doubleValue)
                .orElse(null);
    }

    private Double maxOf(List<GlucoseMeasurementEntity> measurements) {
        return measurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .map(BigDecimal::doubleValue)
                .orElse(null);
    }

    private Double latestValueOf(
            List<GlucoseMeasurementEntity> last24HoursMeasurements,
            List<GlucoseMeasurementEntity> last7DaysMeasurements
    ) {
        return latestMeasurementOf(last24HoursMeasurements).or(() -> latestMeasurementOf(last7DaysMeasurements))
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .map(BigDecimal::doubleValue)
                .orElse(null);
    }

    private java.util.Optional<GlucoseMeasurementEntity> latestMeasurementOf(List<GlucoseMeasurementEntity> measurements) {
        return measurements.stream()
                .filter(measurement -> measurement.getMeasuredAt() != null && measurement.getGlucoseValue() != null)
                .max(Comparator.comparing(GlucoseMeasurementEntity::getMeasuredAt));
    }

    private Integer countBelowThreshold(List<GlucoseMeasurementEntity> measurements, BigDecimal lowThreshold) {
        if (lowThreshold == null || measurements.isEmpty()) {
            return 0;
        }

        return Math.toIntExact(
                measurements.stream()
                        .map(GlucoseMeasurementEntity::getGlucoseValue)
                        .filter(value -> value != null && value.compareTo(lowThreshold) < 0)
                        .count()
        );
    }

    private Integer countAboveThreshold(List<GlucoseMeasurementEntity> measurements, BigDecimal highThreshold) {
        if (highThreshold == null || measurements.isEmpty()) {
            return 0;
        }

        return Math.toIntExact(
                measurements.stream()
                        .map(GlucoseMeasurementEntity::getGlucoseValue)
                        .filter(value -> value != null && value.compareTo(highThreshold) > 0)
                        .count()
        );
    }

    private GlucoseTrend calculateTrend(List<GlucoseMeasurementEntity> last7DaysMeasurements) {
        List<GlucoseMeasurementEntity> safeMeasurements = last7DaysMeasurements == null
                ? List.of()
                : last7DaysMeasurements.stream()
                        .filter(measurement -> measurement.getMeasuredAt() != null && measurement.getGlucoseValue() != null)
                        .sorted(Comparator.comparing(GlucoseMeasurementEntity::getMeasuredAt))
                        .toList();

        if (safeMeasurements.size() < 3) {
            return GlucoseTrend.UNKNOWN;
        }

        int midpoint = safeMeasurements.size() / 2;
        if (midpoint == 0 || midpoint == safeMeasurements.size()) {
            return GlucoseTrend.UNKNOWN;
        }

        List<GlucoseMeasurementEntity> firstHalf = safeMeasurements.subList(0, midpoint);
        List<GlucoseMeasurementEntity> secondHalf = safeMeasurements.subList(midpoint, safeMeasurements.size());

        Double firstHalfAverage = averageOf(firstHalf);
        Double secondHalfAverage = averageOf(secondHalf);
        Double variability = variabilityOf(safeMeasurements);

        if (firstHalfAverage == null || secondHalfAverage == null) {
            return GlucoseTrend.UNKNOWN;
        }

        if (secondHalfAverage >= firstHalfAverage + TREND_DELTA_THRESHOLD) {
            return GlucoseTrend.RISING;
        }
        if (secondHalfAverage <= firstHalfAverage - TREND_DELTA_THRESHOLD) {
            return GlucoseTrend.FALLING;
        }
        if (variability != null && variability >= HIGH_VARIABILITY_THRESHOLD) {
            return GlucoseTrend.VARIABLE;
        }

        return GlucoseTrend.STABLE;
    }

    private RiskLevel calculateRisk(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            double lowThreshold,
            double highThreshold
    ) {
        if (metrics == null) {
            return RiskLevel.LOW;
        }

        int score = 0;

        Double latestValue = metrics.getLatestValue();
        if (latestValue != null) {
            if (latestValue > highThreshold) {
                score += 3;
            }
            if (latestValue < lowThreshold) {
                score += 3;
            }
        }

        Double averageLast24h = metrics.getAverageLast24h();
        if (averageLast24h != null && averageLast24h > highThreshold) {
            score += 2;
        }

        Integer highReadingsCount = metrics.getHighReadingsCount();
        if (highReadingsCount != null && highReadingsCount >= 2) {
            score += 2;
        }

        Integer lowReadingsCount = metrics.getLowReadingsCount();
        if (lowReadingsCount != null && lowReadingsCount >= 1) {
            score += 2;
        }

        if (trend == GlucoseTrend.RISING) {
            score += 2;
        } else if (trend == GlucoseTrend.VARIABLE) {
            score += 1;
        }

        Double variability = metrics.getVariability();
        if (variability != null && variability >= RISK_VARIABILITY_THRESHOLD) {
            score += 1;
        }

        if (score <= 2) {
            return RiskLevel.LOW;
        }
        if (score <= 5) {
            return RiskLevel.MODERATE;
        }
        if (score <= 8) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.CRITICAL;
    }

    private List<String> generateFactors(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            double lowThreshold,
            double highThreshold
    ) {
        List<String> factors = new ArrayList<>();
        if (metrics == null) {
            return factors;
        }

        Double latestValue = metrics.getLatestValue();
        if (latestValue != null) {
            if (latestValue > highThreshold) {
                factors.add("Latest glucose reading is above the configured high threshold");
            } else if (latestValue < lowThreshold) {
                factors.add("Latest glucose reading is below the configured low threshold");
            }
        }

        Integer highReadingsCount = metrics.getHighReadingsCount();
        if (highReadingsCount != null && highReadingsCount >= 2) {
            factors.add("Multiple high glucose readings detected");
        }

        Integer lowReadingsCount = metrics.getLowReadingsCount();
        if (lowReadingsCount != null && lowReadingsCount >= 1) {
            factors.add("One or more low glucose readings detected");
        }

        if (trend == GlucoseTrend.RISING) {
            factors.add("Recent trend is rising");
        } else if (trend == GlucoseTrend.FALLING) {
            factors.add("Recent trend is falling");
        } else if (trend == GlucoseTrend.VARIABLE) {
            factors.add("Recent glucose behavior is variable");
        }

        Double variability = metrics.getVariability();
        if (variability != null && variability >= RISK_VARIABILITY_THRESHOLD) {
            factors.add("High glucose variability detected");
        }

        return factors;
    }

    private List<String> generateRecommendations(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            double lowThreshold,
            double highThreshold
    ) {
        List<String> recommendations = new ArrayList<>();

        if (metrics == null) {
            recommendations.add("Continue consistent monitoring");
            return recommendations;
        }

        Double latestValue = metrics.getLatestValue();
        if (latestValue != null && (latestValue > highThreshold || latestValue < lowThreshold)) {
            recommendations.add("Measure glucose again in the next few hours");
        }

        if (trend == GlucoseTrend.RISING) {
            recommendations.add("Observe whether values increase after meals");
        } else if (trend == GlucoseTrend.FALLING) {
            recommendations.add("Observe whether values decrease after physical activity or fasting periods");
        } else if (trend == GlucoseTrend.VARIABLE) {
            recommendations.add("Look for daily patterns that may explain changing glucose values");
        }

        Double variability = metrics.getVariability();
        if (variability != null && variability >= RISK_VARIABILITY_THRESHOLD) {
            recommendations.add("Continue consistent monitoring");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue consistent monitoring");
        }

        return recommendations;
    }

    private IntelligenceConfidence calculateConfidence(Integer countLast7d) {
        if (countLast7d == null || countLast7d < 3) {
            return IntelligenceConfidence.LOW;
        }
        if (countLast7d < 8) {
            return IntelligenceConfidence.MEDIUM;
        }
        return IntelligenceConfidence.HIGH;
    }

    private AssistantMood determineAssistantMood(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return AssistantMood.INSUFFICIENT_DATA;
        }

        return switch (riskLevel) {
            case LOW -> AssistantMood.CALM;
            case MODERATE -> AssistantMood.ATTENTIVE;
            case HIGH -> AssistantMood.CONCERNED;
            case CRITICAL -> AssistantMood.ALERT;
            case INSUFFICIENT_DATA -> AssistantMood.INSUFFICIENT_DATA;
        };
    }

    private String buildSummary(RiskLevel riskLevel, GlucoseTrend trend, GlucoseAnalysisMetrics metrics) {
        String riskText = riskLevel == null ? "unknown" : riskLevel.name().toLowerCase().replace('_', ' ');
        String trendText = trend == null ? "unknown" : trend.name().toLowerCase().replace('_', ' ');

        if (metrics == null || metrics.getLatestValue() == null) {
            return "Recent glucose data suggests a " + riskText + " risk pattern with a " + trendText + " trend.";
        }

        return String.format(
                "Recent glucose data suggests a %s risk pattern with a %s trend. The latest recorded value was %.1f mg/dL.",
                riskText,
                trendText,
                metrics.getLatestValue()
        );
    }

    private String buildAssistantMessage(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "Continue consistent monitoring while more data is collected.";
        }

        return switch (riskLevel) {
            case LOW -> "Current data looks relatively stable. Continue consistent monitoring.";
            case MODERATE -> "Some changes were detected. Keep monitoring your glucose closely.";
            case HIGH -> "Recent data shows elevated attention signals. Consider checking your glucose again soon.";
            case CRITICAL -> "Recent data shows strong warning signals. Recheck your glucose and stay attentive to how you feel.";
            case INSUFFICIENT_DATA -> "Not enough data is available yet to provide a more detailed analysis.";
        };
    }

    private Double variabilityOf(List<GlucoseMeasurementEntity> measurements) {
        Double min = minOf(measurements);
        Double max = maxOf(measurements);
        return min == null || max == null ? null : max - min;
    }

    private record ThresholdWindow(
            BigDecimal hypoglycemiaThreshold,
            BigDecimal hyperglycemiaThreshold
    ) {
    }
}
