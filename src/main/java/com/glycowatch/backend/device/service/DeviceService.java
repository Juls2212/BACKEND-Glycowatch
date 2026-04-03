package com.glycowatch.backend.device.service;

import com.glycowatch.backend.device.dto.CreateDeviceRequestDto;
import com.glycowatch.backend.device.dto.CreateDeviceResponseDto;
import com.glycowatch.backend.device.dto.DeviceResponseDto;
import com.glycowatch.backend.device.dto.LinkDeviceResponseDto;
import com.glycowatch.backend.device.dto.ToggleDeviceResponseDto;
import java.util.List;

public interface DeviceService {

    List<DeviceResponseDto> listDevices(String authenticatedEmail);

    CreateDeviceResponseDto registerDevice(String authenticatedEmail, CreateDeviceRequestDto request);

    LinkDeviceResponseDto linkDevice(String authenticatedEmail, Long deviceId);

    ToggleDeviceResponseDto toggleDevice(String authenticatedEmail, Long deviceId);
}


