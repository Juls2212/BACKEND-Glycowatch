package com.glycowatch.device.dto;

import com.glycowatch.device.model.DeviceStatus;

public record ToggleDeviceResponseDto(
        Long deviceId,
        boolean active,
        DeviceStatus status
) {
}



