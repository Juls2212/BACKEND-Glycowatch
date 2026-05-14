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
                                    "Eres un asistente inteligente de monitoreo de glucosa. "
                                            + "Debes responder en espanol claro, natural, breve y util. "
                                            + "Responde SOLO con JSON valido y no agregues texto fuera del JSON. "
                                            + "No des diagnosticos. No indiques medicamentos. No indiques dosis. "
                                            + "No repitas mecanicamente el riesgo calculado; agrega interpretacion breve y segura."
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
                  "explanation": "explicacion breve en espanol",
                  "assistantMessage": "mensaje cercano y claro en espanol para la persona usuaria",
                  "recommendations": ["...", "..."]
                }

                Las recomendaciones deben ser breves, seguras y en espanol.
                Los textos deben sonar como un asistente inteligente de monitoreo, no como una plantilla generica.

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
