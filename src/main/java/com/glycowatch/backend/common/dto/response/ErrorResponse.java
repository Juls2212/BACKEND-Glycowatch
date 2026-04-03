package com.glycowatch.backend.common.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private final boolean success;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final String path;
}


