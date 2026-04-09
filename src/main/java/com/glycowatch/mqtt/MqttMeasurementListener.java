package com.glycowatch.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glycowatch.common.exception.ApiException;
import com.glycowatch.measurement.dto.IngestMeasurementRequestDto;
import com.glycowatch.measurement.service.MeasurementIngestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class MqttMeasurementListener {

    private static final String MEASUREMENT_UNIT = "mg/dL";

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final MeasurementIngestionService measurementIngestionService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleIncomingMessage(Message<?> message) {
        String topic = resolveTopic(message.getHeaders());
        String payload = message.getPayload() == null ? "" : message.getPayload().toString();

        log.info("MQTT message received on topic={} payloadLength={}", topic, payload.length());

        MqttMeasurementMessageDto mqttMessage;
        try {
            mqttMessage = objectMapper.readValue(payload, MqttMeasurementMessageDto.class);
        } catch (JsonProcessingException ex) {
            log.warn("MQTT invalid payload on topic={}: malformed JSON", topic);
            return;
        }

        Set<ConstraintViolation<MqttMeasurementMessageDto>> violations = validator.validate(mqttMessage);
        if (!violations.isEmpty()) {
            String validationSummary = violations.stream()
                    .map(violation -> violation.getPropertyPath() + "=" + violation.getMessage())
                    .collect(Collectors.joining(", "));
            log.warn("MQTT invalid payload on topic={} deviceIdentifier={} violations={}",
                    topic,
                    mqttMessage.deviceIdentifier(),
                    validationSummary);
            return;
        }

        try {
            measurementIngestionService.ingestMeasurement(
                    mqttMessage.deviceIdentifier(),
                    mqttMessage.apiKey(),
                    new IngestMeasurementRequestDto(
                            mqttMessage.glucoseMgDl(),
                            MEASUREMENT_UNIT,
                            mqttMessage.measuredAt()
                    )
            );
            log.info("MQTT measurement ingested successfully topic={} deviceIdentifier={} sourceEventId={}",
                    topic,
                    mqttMessage.deviceIdentifier(),
                    mqttMessage.sourceEventId());
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED || ex.getStatus() == HttpStatus.FORBIDDEN) {
                log.warn("MQTT device authentication failed topic={} deviceIdentifier={} errorCode={}",
                        topic,
                        mqttMessage.deviceIdentifier(),
                        ex.getErrorCode());
                return;
            }
            if (ex.getStatus().is4xxClientError()) {
                log.warn("MQTT invalid payload topic={} deviceIdentifier={} errorCode={} message={}",
                        topic,
                        mqttMessage.deviceIdentifier(),
                        ex.getErrorCode(),
                        ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    private String resolveTopic(MessageHeaders headers) {
        Object receivedTopic = headers.get("mqtt_receivedTopic");
        return receivedTopic == null ? "unknown" : receivedTopic.toString();
    }
}
