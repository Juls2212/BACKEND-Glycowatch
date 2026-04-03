package com.glycowatch.backend.device.dto;

import com.glycowatch.backend.device.model.DeviceStatus;

public record DeviceResponseDto(
        Long id,
        String name,
        String identifier,
        DeviceStatus status,
        boolean active
) {
}


