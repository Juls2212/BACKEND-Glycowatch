package com.glycowatch.intelligence.controller;

import com.glycowatch.common.dto.response.ApiResponse;
import com.glycowatch.intelligence.dto.IntelligenceSummaryResponse;
import com.glycowatch.intelligence.service.IntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
@Tag(name = "Intelligence", description = "Intelligence endpoints")
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    @GetMapping("/summary")
    @Operation(summary = "Get intelligence summary for authenticated user")
    public ResponseEntity<ApiResponse<IntelligenceSummaryResponse>> getSummary(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        IntelligenceSummaryResponse data = intelligenceService.getSummary(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<IntelligenceSummaryResponse>builder()
                        .success(true)
                        .message("Intelligence summary retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}
