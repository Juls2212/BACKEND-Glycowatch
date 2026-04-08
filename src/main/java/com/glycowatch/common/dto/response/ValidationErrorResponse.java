package com.glycowatch.common.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ValidationErrorResponse {

    private final boolean success;
    private final String error;
    private final String message;
    private final List<FieldViolation> violations;
    private final Instant timestamp;
    private final String path;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FieldViolation {
        private final String field;
        private final String message;
    }
}



