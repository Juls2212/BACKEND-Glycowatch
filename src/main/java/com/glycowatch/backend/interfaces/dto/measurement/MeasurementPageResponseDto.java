package com.glycowatch.backend.interfaces.dto.measurement;

import java.util.List;

public record MeasurementPageResponseDto(
        List<MeasurementResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage
) {
}

