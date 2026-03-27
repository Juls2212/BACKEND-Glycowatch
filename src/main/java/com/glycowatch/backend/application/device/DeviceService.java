package com.glycowatch.backend.application.device;

import com.glycowatch.backend.interfaces.dto.device.CreateDeviceRequestDto;
import com.glycowatch.backend.interfaces.dto.device.CreateDeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.DeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.LinkDeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.ToggleDeviceResponseDto;
import java.util.List;

public interface DeviceService {

    List<DeviceResponseDto> listDevices(String authenticatedEmail);

    CreateDeviceResponseDto registerDevice(String authenticatedEmail, CreateDeviceRequestDto request);

    LinkDeviceResponseDto linkDevice(String authenticatedEmail, Long deviceId);

    ToggleDeviceResponseDto toggleDevice(String authenticatedEmail, Long deviceId);
}

