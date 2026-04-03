package com.glycowatch.backend.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequestDto(
        @NotBlank(message = "Name is required.")
        @Size(max = 255, message = "Name cannot exceed 255 characters.")
        String name,

        @NotBlank(message = "Identifier is required.")
        @Size(max = 255, message = "Identifier cannot exceed 255 characters.")
        String identifier
) {
}


