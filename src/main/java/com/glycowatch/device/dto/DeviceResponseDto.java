package com.glycowatch.device.dto;

import com.glycowatch.device.model.DeviceStatus;

public record DeviceResponseDto(
        Long id,
        String name,
        String identifier,
        DeviceStatus status,
        boolean active
) {
}



