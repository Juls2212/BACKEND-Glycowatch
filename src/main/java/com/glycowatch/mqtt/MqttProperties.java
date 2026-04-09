package com.glycowatch.mqtt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProperties(
        boolean enabled,
        @NotBlank(message = "MQTT broker URL is required.")
        String brokerUrl,
        @NotBlank(message = "MQTT client id is required.")
        String clientId,
        @NotBlank(message = "MQTT topic is required.")
        String topic,
        String username,
        String password,
        @Min(value = 1000, message = "MQTT completion timeout must be at least 1000 ms.")
        long completionTimeoutMs,
        @Min(value = 1000, message = "MQTT reconnect delay must be at least 1000 ms.")
        long reconnectDelayMs,
        @Min(value = 0, message = "MQTT QoS must be between 0 and 2.")
        @Max(value = 2, message = "MQTT QoS must be between 0 and 2.")
        int qos
) {
}
