package com.glycowatch.device.dto;

import java.time.Instant;

public record LinkDeviceResponseDto(
        Long deviceId,
        boolean linked,
        Instant linkedAt
) {
}



