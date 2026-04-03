package com.glycowatch.backend.measurement.controller;

import com.glycowatch.backend.measurement.service.MeasurementQueryService;
import com.glycowatch.backend.measurement.service.MeasurementManualService;
import com.glycowatch.backend.measurement.dto.IngestMeasurementResponseDto;
import com.glycowatch.backend.measurement.dto.LatestMeasurementResponseDto;
import com.glycowatch.backend.measurement.dto.ManualMeasurementRequestDto;
import com.glycowatch.backend.measurement.dto.MeasurementPageResponseDto;
import com.glycowatch.backend.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
@Validated
@Tag(name = "Measurements", description = "Measurement query endpoints")
public class MeasurementController {

    private final MeasurementQueryService measurementQueryService;
    private final MeasurementManualService measurementManualService;

    @GetMapping
    @Operation(summary = "List measurements with pagination and optional date range")
    public ResponseEntity<ApiResponse<MeasurementPageResponseDto>> getMeasurements(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest
    ) {
        MeasurementPageResponseDto data = measurementQueryService.getMeasurements(authentication.getName(), page, size, from, to);
        return ResponseEntity.ok(
                ApiResponse.<MeasurementPageResponseDto>builder()
                        .success(true)
                        .message("Measurements retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest valid measurement")
    public ResponseEntity<ApiResponse<LatestMeasurementResponseDto>> getLatestMeasurement(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        LatestMeasurementResponseDto data = measurementQueryService.getLatestMeasurement(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<LatestMeasurementResponseDto>builder()
                        .success(true)
                        .message("Latest measurement retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PostMapping("/manual")
    @Operation(summary = "Create manual glucose measurement for authenticated user")
    public ResponseEntity<ApiResponse<IngestMeasurementResponseDto>> createManualMeasurement(
            Authentication authentication,
            @Valid @RequestBody ManualMeasurementRequestDto request,
            HttpServletRequest httpRequest
    ) {
        IngestMeasurementResponseDto data = measurementManualService.createManualMeasurement(authentication.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.<IngestMeasurementResponseDto>builder()
                        .success(true)
                        .message("Manual measurement created successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete measurement for authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteMeasurement(
            Authentication authentication,
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        measurementQueryService.deleteMeasurement(authentication.getName(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Measurement deleted successfully.")
                        .data(null)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}

