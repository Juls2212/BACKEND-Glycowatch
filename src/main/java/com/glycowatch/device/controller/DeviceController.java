package com.glycowatch.device.controller;

import com.glycowatch.device.service.DeviceService;
import com.glycowatch.device.dto.CreateDeviceRequestDto;
import com.glycowatch.device.dto.CreateDeviceResponseDto;
import com.glycowatch.device.dto.DeviceResponseDto;
import com.glycowatch.device.dto.LinkDeviceResponseDto;
import com.glycowatch.device.dto.RemoveDeviceResponseDto;
import com.glycowatch.device.dto.ToggleDeviceResponseDto;
import com.glycowatch.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Device management endpoints")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "List devices linked to authenticated user")
    public ResponseEntity<ApiResponse<List<DeviceResponseDto>>> listDevices(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<DeviceResponseDto> data = deviceService.listDevices(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<List<DeviceResponseDto>>builder()
                        .success(true)
                        .message("Devices retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PostMapping
    @Operation(summary = "Register a new device")
    public ResponseEntity<ApiResponse<CreateDeviceResponseDto>> registerDevice(
            Authentication authentication,
            @Valid @RequestBody CreateDeviceRequestDto request,
            HttpServletRequest httpRequest
    ) {
        CreateDeviceResponseDto data = deviceService.registerDevice(authentication.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.<CreateDeviceResponseDto>builder()
                        .success(true)
                        .message("Device registered successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PostMapping("/{id}/link")
    @Operation(summary = "Link an existing device to authenticated user")
    public ResponseEntity<ApiResponse<LinkDeviceResponseDto>> linkDevice(
            Authentication authentication,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        LinkDeviceResponseDto data = deviceService.linkDevice(authentication.getName(), id);
        return ResponseEntity.ok(
                ApiResponse.<LinkDeviceResponseDto>builder()
                        .success(true)
                        .message("Device linked successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable a linked device")
    public ResponseEntity<ApiResponse<ToggleDeviceResponseDto>> toggleDevice(
            Authentication authentication,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        ToggleDeviceResponseDto data = deviceService.toggleDevice(authentication.getName(), id);
        return ResponseEntity.ok(
                ApiResponse.<ToggleDeviceResponseDto>builder()
                        .success(true)
                        .message("Device status updated successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a linked device from active management")
    public ResponseEntity<ApiResponse<RemoveDeviceResponseDto>> removeDevice(
            Authentication authentication,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        RemoveDeviceResponseDto data = deviceService.removeDevice(authentication.getName(), id);
        return ResponseEntity.ok(
                ApiResponse.<RemoveDeviceResponseDto>builder()
                        .success(true)
                        .message("Device removed from active management successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}


