package com.glycowatch.backend.analytics.controller;

import com.glycowatch.backend.analytics.service.DashboardService;
import com.glycowatch.backend.analytics.service.RiskAnalysisService;
import com.glycowatch.backend.analytics.dto.ChartPointDto;
import com.glycowatch.backend.analytics.dto.DashboardResponseDto;
import com.glycowatch.backend.analytics.dto.RiskAnalysisResponseDto;
import com.glycowatch.backend.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Simple analytics endpoints")
public class AnalyticsController {

    private final DashboardService dashboardService;
    private final RiskAnalysisService riskAnalysisService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get simple dashboard analytics")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboard(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DashboardResponseDto data = dashboardService.getDashboard(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<DashboardResponseDto>builder()
                        .success(true)
                        .message("Dashboard retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @GetMapping("/chart")
    @Operation(summary = "Get chart-ready glucose points")
    public ResponseEntity<ApiResponse<List<ChartPointDto>>> getChartData(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<ChartPointDto> data = dashboardService.getChartData(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<List<ChartPointDto>>builder()
                        .success(true)
                        .message("Chart data retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @GetMapping("/risk")
    @Operation(summary = "Get current risk analysis")
    public ResponseEntity<ApiResponse<RiskAnalysisResponseDto>> getRiskAnalysis(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        RiskAnalysisResponseDto data = riskAnalysisService.getRiskAnalysis(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<RiskAnalysisResponseDto>builder()
                        .success(true)
                        .message("Risk analysis retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}

