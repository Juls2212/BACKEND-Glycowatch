package com.glycowatch.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glycowatch.intelligence.integration.GeminiAnalysisResult;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.RiskLevel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class OpenAIIntelligenceProvider implements ExternalIntelligenceProvider {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    private final ObjectMapper objectMapper;
    private final GeminiIntelligenceProvider geminiIntelligenceProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isAvailable() {
        return hasOpenAiApiKey() || geminiIntelligenceProvider.isAvailable();
    }

    @Override
    public Optional<GeminiAnalysisResult> generateGlucoseAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    ) {
        if (hasOpenAiApiKey()) {
            Optional<GeminiAnalysisResult> openAiResult = generateWithOpenAi(
                    metrics,
                    trend,
                    ruleBasedRiskLevel,
                    detectedFactors,
                    currentRecommendations
            );
            if (openAiResult.isPresent()) {
                return openAiResult;
            }
        }

        return geminiIntelligenceProvider.generateGlucoseAnalysis(
                metrics,
                trend,
                ruleBasedRiskLevel,
                detectedFactors,
                currentRecommendations
        );
    }

    private Optional<GeminiAnalysisResult> generateWithOpenAi(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    ) {
        if (metrics == null || trend == null || ruleBasedRiskLevel == null) {
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(normalizedApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", DEFAULT_MODEL,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    "Eres el asistente inteligente de GlycoWatch para monitoreo de glucosa. "
                                            + "Tu trabajo es interpretar patrones recientes de glucosa con lenguaje claro, natural, humano y completamente en espanol. "
                                            + "Debes sonar como un asistente atento, util y cercano, no como un reporte tecnico ni como una plantilla automatica. "
                                            + "Resume el comportamiento reciente, destaca cambios importantes y explica tendencias de forma breve y comprensible. "
                                            + "Aporta valor interpretativo; no te limites a repetir el riesgo calculado por reglas. "
                                            + "Responde SOLO con JSON valido y no agregues texto fuera del JSON. "
                                            + "No des diagnosticos. No indiques medicamentos. No indiques dosis. "
                                            + "Las recomendaciones deben ser breves, seguras, practicas y apropiadas para tarjetas de interfaz."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", buildPrompt(
                                            metrics,
                                            trend,
                                            ruleBasedRiskLevel,
                                            detectedFactors,
                                            currentRecommendations
                                    )
                            )
                    )
            );

            ResponseEntity<OpenAiChatCompletionResponse> response = restTemplate.postForEntity(
                    OPENAI_URL,
                    new HttpEntity<>(requestBody, headers),
                    OpenAiChatCompletionResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            return extractResult(response.getBody());
        } catch (RestClientException ex) {
            log.warn("OpenAI request failed: {}", ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("OpenAI response handling failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeminiAnalysisResult> extractResult(OpenAiChatCompletionResponse response) {
        if (response.choices() == null || response.choices().isEmpty()) {
            return Optional.empty();
        }

        OpenAiChoice choice = response.choices().getFirst();
        if (choice.message() == null || !StringUtils.hasText(choice.message().content())) {
            return Optional.empty();
        }

        try {
            GeminiAnalysisResult result = objectMapper.readValue(choice.message().content(), GeminiAnalysisResult.class);
            return isValidResult(result) ? Optional.of(result) : Optional.empty();
        } catch (Exception ex) {
            log.warn("OpenAI JSON parsing failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isValidResult(GeminiAnalysisResult result) {
        if (result == null
                || !StringUtils.hasText(result.getRiskLevel())
                || !StringUtils.hasText(result.getExplanation())
                || !StringUtils.hasText(result.getAssistantMessage())
                || result.getRecommendations() == null
                || result.getRecommendations().stream().anyMatch(recommendation -> !StringUtils.hasText(recommendation))) {
            return false;
        }

        try {
            RiskLevel.valueOf(result.getRiskLevel().trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String buildPrompt(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    ) {
        return """
                Usa exactamente esta estructura JSON:
                {
                  "riskLevel": "LOW|MODERATE|HIGH|CRITICAL|INSUFFICIENT_DATA",
                  "explanation": "explicacion breve, natural y contextual en espanol",
                  "assistantMessage": "mensaje breve, cercano y util en espanol",
                  "recommendations": ["...", "..."]
                }

                Reglas de estilo:
                - Todo el contenido debe estar en espanol natural.
                - Explica el comportamiento reciente de la glucosa, no solo el nivel de riesgo.
                - Si hay tendencia o variabilidad, interpretala con lenguaje humano y claro.
                - Evita frases roboticas, literales o repetitivas.
                - No copies textualmente los factores ni las recomendaciones actuales; reescribelos con mejor contexto si ayuda.
                - Las recomendaciones deben ser concretas, breves y seguras.
                - El assistantMessage debe sentirse como la voz principal del asistente.

                Datos de entrada:
                - latest glucose value: %s
                - averageLast24h: %s
                - averageLast7d: %s
                - highReadingsCount: %s
                - lowReadingsCount: %s
                - variability: %s
                - trend: %s
                - ruleBasedRiskLevel: %s
                - detectedFactors: %s
                - currentRecommendations: %s
                
                Objetivo:
                - "explanation" debe resumir lo mas importante del comportamiento reciente.
                - "assistantMessage" debe sonar como una orientacion breve del asistente.
                - "recommendations" debe contener 2 o 3 sugerencias cortas, utiles y seguras.
                """.formatted(
                valueOrNull(metrics.getLatestValue()),
                valueOrNull(metrics.getAverageLast24h()),
                valueOrNull(metrics.getAverageLast7d()),
                valueOrNull(metrics.getHighReadingsCount()),
                valueOrNull(metrics.getLowReadingsCount()),
                valueOrNull(metrics.getVariability()),
                trend.name(),
                ruleBasedRiskLevel.name(),
                listOrEmpty(detectedFactors),
                listOrEmpty(currentRecommendations)
        );
    }

    private boolean hasOpenAiApiKey() {
        return StringUtils.hasText(normalizedApiKey());
    }

    private String normalizedApiKey() {
        return openAiApiKey == null ? null : openAiApiKey.trim();
    }

    private String valueOrNull(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String listOrEmpty(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }

    private record OpenAiChatCompletionResponse(List<OpenAiChoice> choices) {
    }

    private record OpenAiChoice(OpenAiMessage message) {
    }

    private record OpenAiMessage(String content) {
    }
}
