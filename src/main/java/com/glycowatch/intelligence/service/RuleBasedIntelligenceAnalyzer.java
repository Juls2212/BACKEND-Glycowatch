package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.model.AssistantMood;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIntelligenceAnalyzer {

    private static final double TREND_DELTA_THRESHOLD = 15.0;
    private static final double HIGH_VARIABILITY_THRESHOLD = 40.0;
    private static final double RISK_VARIABILITY_THRESHOLD = 80.0;

    public RuleBasedAnalysis analyze(
            List<GlucoseMeasurementEntity> last24HoursMeasurements,
            List<GlucoseMeasurementEntity> last7DaysMeasurements,
            BigDecimal lowThreshold,
            BigDecimal highThreshold
    ) {
        GlucoseAnalysisMetrics metrics = computeMetrics(
                last24HoursMeasurements,
                last7DaysMeasurements,
                lowThreshold,
                highThreshold
        );
        GlucoseTrend trend = calculateTrend(last7DaysMeasurements);
        double safeLowThreshold = lowThreshold == null ? 0.0 : lowThreshold.doubleValue();
        double safeHighThreshold = highThreshold == null ? 0.0 : highThreshold.doubleValue();
        RiskLevel riskLevel = calculateRisk(metrics, trend, safeLowThreshold, safeHighThreshold);
        IntelligenceConfidence confidence = calculateConfidence(metrics.getCountLast7d());
        AssistantMood assistantMood = determineAssistantMood(riskLevel);
        List<String> detectedFactors = generateFactors(metrics, trend, safeLowThreshold, safeHighThreshold);
        List<String> recommendations = generateRecommendations(metrics, trend, safeLowThreshold, safeHighThreshold);
        String summary = buildSummary(riskLevel, trend, metrics);

        return new RuleBasedAnalysis(
                metrics,
                trend,
                riskLevel,
                confidence,
                assistantMood,
                detectedFactors,
                recommendations,
                summary
        );
    }

    public AssistantMood determineAssistantMood(RiskLevel riskLevel) {
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
                factors.add("La ultima lectura de glucosa esta por encima del umbral alto configurado");
            } else if (latestValue < lowThreshold) {
                factors.add("La ultima lectura de glucosa esta por debajo del umbral bajo configurado");
            }
        }

        Integer highReadingsCount = metrics.getHighReadingsCount();
        if (highReadingsCount != null && highReadingsCount >= 2) {
            factors.add("Se detectaron varias lecturas altas de glucosa");
        }

        Integer lowReadingsCount = metrics.getLowReadingsCount();
        if (lowReadingsCount != null && lowReadingsCount >= 1) {
            factors.add("Se detecto al menos una lectura baja de glucosa");
        }

        if (trend == GlucoseTrend.RISING) {
            factors.add("La tendencia reciente va en aumento");
        } else if (trend == GlucoseTrend.FALLING) {
            factors.add("La tendencia reciente va en descenso");
        } else if (trend == GlucoseTrend.VARIABLE) {
            factors.add("El comportamiento reciente de la glucosa es variable");
        }

        Double variability = metrics.getVariability();
        if (variability != null && variability >= RISK_VARIABILITY_THRESHOLD) {
            factors.add("Se detecto una variabilidad alta en la glucosa");
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
            recommendations.add("Continua con un monitoreo constante");
            return recommendations;
        }

        Double latestValue = metrics.getLatestValue();
        if (latestValue != null && (latestValue > highThreshold || latestValue < lowThreshold)) {
            recommendations.add("Vuelve a medir tu glucosa en las proximas horas");
        }

        if (trend == GlucoseTrend.RISING) {
            recommendations.add("Observa si los valores aumentan despues de las comidas");
        } else if (trend == GlucoseTrend.FALLING) {
            recommendations.add("Observa si los valores disminuyen despues de actividad fisica o periodos de ayuno");
        } else if (trend == GlucoseTrend.VARIABLE) {
            recommendations.add("Busca patrones diarios que puedan explicar los cambios en la glucosa");
        }

        Double variability = metrics.getVariability();
        if (variability != null && variability >= RISK_VARIABILITY_THRESHOLD) {
            recommendations.add("Manten un seguimiento frecuente para confirmar la evolucion");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continua con un monitoreo constante");
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

    private String buildSummary(RiskLevel riskLevel, GlucoseTrend trend, GlucoseAnalysisMetrics metrics) {
        String riskText = riskLevel == null ? "desconocido" : toSpanishRiskText(riskLevel);
        String trendText = trend == null ? "desconocida" : toSpanishTrendText(trend);

        if (metrics == null || metrics.getLatestValue() == null) {
            return "Los datos recientes sugieren un nivel de riesgo " + riskText + " con una tendencia " + trendText + ".";
        }

        return String.format(
                "Los datos recientes sugieren un nivel de riesgo %s con una tendencia %s. La ultima lectura registrada fue de %.1f mg/dL.",
                riskText,
                trendText,
                metrics.getLatestValue()
        );
    }

    private String toSpanishRiskText(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> "bajo";
            case MODERATE -> "moderado";
            case HIGH -> "alto";
            case CRITICAL -> "critico";
            case INSUFFICIENT_DATA -> "insuficiente";
        };
    }

    private String toSpanishTrendText(GlucoseTrend trend) {
        return switch (trend) {
            case STABLE -> "estable";
            case RISING -> "en aumento";
            case FALLING -> "en descenso";
            case VARIABLE -> "variable";
            case UNKNOWN -> "no concluyente";
        };
    }

    private Double variabilityOf(List<GlucoseMeasurementEntity> measurements) {
        Double min = minOf(measurements);
        Double max = maxOf(measurements);
        return min == null || max == null ? null : max - min;
    }

    public record RuleBasedAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel riskLevel,
            IntelligenceConfidence confidence,
            AssistantMood assistantMood,
            List<String> detectedFactors,
            List<String> recommendations,
            String summary
    ) {
        public boolean hasSufficientData() {
            return metrics != null && metrics.getCountLast7d() != null && metrics.getCountLast7d() >= 3;
        }
    }
}
