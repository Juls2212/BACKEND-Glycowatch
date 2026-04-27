package com.glycowatch.intelligence.integration;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GeminiClient {

    private final GeminiProperties geminiProperties;

    public GeminiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    public boolean isAvailable() {
        return geminiProperties.apiKey() != null && !geminiProperties.apiKey().isBlank();
    }

    public Optional<String> generateContent(String prompt) {
        if (!isAvailable() || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public String getModel() {
        return geminiProperties.model();
    }

    public String getBaseUrl() {
        return geminiProperties.baseUrl();
    }
}
