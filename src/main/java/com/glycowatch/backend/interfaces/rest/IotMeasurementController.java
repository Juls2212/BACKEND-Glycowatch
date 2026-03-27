package com.glycowatch.backend.interfaces.rest;

import com.glycowatch.backend.application.measurement.MeasurementIngestionService;
import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementRequestDto;
import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementResponseDto;
import com.glycowatch.backend.interfaces.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iot/measurements")
@RequiredArgsConstructor
@Tag(name = "IoT Measurements", description = "ESP32 ingestion endpoint")
public class IotMeasurementController {

    private final MeasurementIngestionService measurementIngestionService;

    @PostMapping
    @Operation(summary = "Ingest glucose measurement from IoT device")
    public ResponseEntity<ApiResponse<IngestMeasurementResponseDto>> ingest(
            @RequestHeader(name = "X-DEVICE-ID", required = false) String deviceIdHeader,
            @RequestHeader(name = "X-DEVICE-KEY", required = false) String deviceKeyHeader,
            @Valid @RequestBody IngestMeasurementRequestDto request,
            HttpServletRequest httpRequest
    ) {
        IngestMeasurementResponseDto data = measurementIngestionService.ingestMeasurement(deviceIdHeader, deviceKeyHeader, request);
        return ResponseEntity.ok(
                ApiResponse.<IngestMeasurementResponseDto>builder()
                        .success(true)
                        .message("Measurement ingested successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}

