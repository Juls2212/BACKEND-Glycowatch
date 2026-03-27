package com.glycowatch.backend.interfaces.rest;

import com.glycowatch.backend.application.alert.AlertService;
import com.glycowatch.backend.interfaces.dto.alert.AlertResponseDto;
import com.glycowatch.backend.interfaces.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert management endpoints")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "List alerts for authenticated user")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getAlerts(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<AlertResponseDto> data = alertService.getAlerts(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<List<AlertResponseDto>>builder()
                        .success(true)
                        .message("Alerts retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark alert as read")
    public ResponseEntity<ApiResponse<AlertResponseDto>> markAsRead(
            Authentication authentication,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        AlertResponseDto data = alertService.markAsRead(authentication.getName(), id);
        return ResponseEntity.ok(
                ApiResponse.<AlertResponseDto>builder()
                        .success(true)
                        .message("Alert marked as read.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}

