package com.glycowatch.device.service;

import com.glycowatch.device.dto.CreateDeviceRequestDto;
import com.glycowatch.device.dto.CreateDeviceResponseDto;
import com.glycowatch.device.dto.DeviceResponseDto;
import com.glycowatch.device.dto.LinkDeviceResponseDto;
import com.glycowatch.device.dto.RemoveDeviceResponseDto;
import com.glycowatch.device.dto.ToggleDeviceResponseDto;
import java.util.List;

public interface DeviceService {

    List<DeviceResponseDto> listDevices(String authenticatedEmail);

    CreateDeviceResponseDto registerDevice(String authenticatedEmail, CreateDeviceRequestDto request);

    LinkDeviceResponseDto linkDevice(String authenticatedEmail, Long deviceId);

    ToggleDeviceResponseDto toggleDevice(String authenticatedEmail, Long deviceId);

    RemoveDeviceResponseDto removeDevice(String authenticatedEmail, Long deviceId);
}


