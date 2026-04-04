package com.glycowatch.backend.measurement.dto;

import java.util.List;

// DTO for paginated measurement responses
public record MeasurementPageResponseDto(
        List<MeasurementResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage
) {
}


