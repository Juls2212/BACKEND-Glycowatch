package com.glycowatch.backend.device.dto;

import com.glycowatch.backend.device.model.DeviceStatus;

public record ToggleDeviceResponseDto(
        Long deviceId,
        boolean active,
        DeviceStatus status
) {
}


