package com.glycowatch.intelligence.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glycowatch.intelligence.model.GlucoseAnalysisMetrics;
import com.glycowatch.intelligence.model.GlucoseTrend;
import com.glycowatch.intelligence.model.RiskLevel;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    void logConfiguration() {
        log.info(
                "Gemini configuration loaded. apiKeyPresent={}, model={}, baseUrl={}",
                isAvailable(),
                getModel(),
                getBaseUrl()
        );
    }

    public boolean isAvailable() {
        return StringUtils.hasText(normalizedApiKey());
    }

    public Optional<String> generateContent(String prompt) {
        if (!isAvailable() || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        try {
            String endpoint = UriComponentsBuilder
                    .fromHttpUrl(normalizedApiBaseUrl())
                    .pathSegment("models", geminiProperties.model() + ":generateContent")
                    .queryParam("key", normalizedApiKey())
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            GeminiGenerateContentRequest requestBody = new GeminiGenerateContentRequest(
                    List.of(new GeminiContent(List.of(new GeminiPart(prompt))))
            );

            ResponseEntity<GeminiGenerateContentResponse> response = restTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<>(requestBody, headers),
                    GeminiGenerateContentResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            return extractContent(response.getBody());
        } catch (RestClientException ex) {
            log.warn("Gemini request failed: {}", ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Gemini response handling failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<GeminiAnalysisResult> generateGlucoseAnalysis(
            GlucoseAnalysisMetrics metrics,
            GlucoseTrend trend,
            RiskLevel ruleBasedRiskLevel,
            List<String> detectedFactors,
            List<String> currentRecommendations
    ) {
        if (!isAvailable() || metrics == null || trend == null || ruleBasedRiskLevel == null) {
            return Optional.empty();
        }

        String prompt = buildPrompt(metrics, trend, ruleBasedRiskLevel, detectedFactors, currentRecommendations);
        return generateContent(prompt).flatMap(this::parseAnalysisResult);
    }

    public String getModel() {
        return geminiProperties.model();
    }

    public String getBaseUrl() {
        return normalizedApiBaseUrl();
    }

    private Optional<String> extractContent(GeminiGenerateContentResponse response) {
        if (response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }

        GeminiCandidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return Optional.empty();
        }

        String text = candidate.content().parts().stream()
                .map(GeminiPart::text)
                .filter(StringUtils::hasText)
                .reduce("", String::concat);

        String normalized = stripMarkdownCodeFences(text);
        return StringUtils.hasText(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    private Optional<GeminiAnalysisResult> parseAnalysisResult(String json) {
        try {
            GeminiAnalysisResult result = objectMapper.readValue(json, GeminiAnalysisResult.class);
            if (!isValidResult(result)) {
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (Exception ex) {
            log.warn("Gemini JSON parsing failed: {}", ex.getMessage());
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
                You are assisting with a glucose trend interpretation task.
                Respond ONLY with valid JSON. Do not include explanations outside JSON.
                Do not provide a diagnosis.
                Do not provide medication instructions.
                Do not provide dosage advice.
                Use this exact JSON structure:
                {
                  "riskLevel": "LOW|MODERATE|HIGH|CRITICAL|INSUFFICIENT_DATA",
                  "explanation": "short explanation in English",
                  "assistantMessage": "friendly message for the user",
                  "recommendations": ["...", "..."]
                }

                Input data:
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

    private String valueOrNull(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String normalizedApiKey() {
        return geminiProperties.apiKey() == null ? null : geminiProperties.apiKey().trim();
    }

    private String normalizedApiBaseUrl() {
        String baseUrl = geminiProperties.baseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "https://generativelanguage.googleapis.com/v1beta";
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!normalized.endsWith("/v1beta")) {
            normalized = normalized + "/v1beta";
        }

        return normalized;
    }

    private String listOrEmpty(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }

    private String stripMarkdownCodeFences(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        String normalized = value.trim();
        if (normalized.startsWith("```")) {
            int firstNewLine = normalized.indexOf('\n');
            if (firstNewLine >= 0) {
                normalized = normalized.substring(firstNewLine + 1);
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3);
            }
        }
        return normalized.trim();
    }

    private record GeminiGenerateContentRequest(List<GeminiContent> contents) {
    }

    private record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }
}
