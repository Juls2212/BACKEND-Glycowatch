package com.glycowatch.backend.interfaces.dto.device;

import java.time.Instant;

public record LinkDeviceResponseDto(
        Long deviceId,
        boolean linked,
        Instant linkedAt
) {
}

