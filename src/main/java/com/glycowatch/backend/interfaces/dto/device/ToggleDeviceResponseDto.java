package com.glycowatch.backend.interfaces.dto.device;

import com.glycowatch.backend.domain.device.DeviceStatus;

public record ToggleDeviceResponseDto(
        Long deviceId,
        boolean active,
        DeviceStatus status
) {
}

