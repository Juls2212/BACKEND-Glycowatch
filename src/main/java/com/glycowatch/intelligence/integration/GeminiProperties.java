package com.glycowatch.intelligence.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl
) {
}
