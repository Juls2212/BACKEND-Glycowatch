package com.glycowatch.backend.interfaces.dto.device;

public record CreateDeviceResponseDto(
        Long deviceId,
        String apiKey
) {
}

