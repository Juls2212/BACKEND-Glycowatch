package com.glycowatch.backend.interfaces.dto.device;

import com.glycowatch.backend.domain.device.DeviceStatus;

public record DeviceResponseDto(
        Long id,
        String name,
        String identifier,
        DeviceStatus status,
        boolean active
) {
}

