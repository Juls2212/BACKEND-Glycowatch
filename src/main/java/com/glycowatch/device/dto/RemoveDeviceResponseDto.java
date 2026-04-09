package com.glycowatch.device.dto;

import com.glycowatch.device.model.DeviceStatus;
import java.time.Instant;

public record RemoveDeviceResponseDto(
        Long deviceId,
        boolean removed,
        DeviceStatus status,
        Instant removedAt
) {
}
