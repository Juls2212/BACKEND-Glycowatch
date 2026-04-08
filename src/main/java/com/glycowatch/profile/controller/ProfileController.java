package com.glycowatch.profile.controller;

import com.glycowatch.profile.service.ProfileService;
import com.glycowatch.profile.dto.ProfileResponseDto;
import com.glycowatch.profile.dto.UpdateProfileRequestDto;
import com.glycowatch.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile management endpoints")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<ApiResponse<ProfileResponseDto>> getProfile(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ProfileResponseDto data = profileService.getProfile(authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<ProfileResponseDto>builder()
                        .success(true)
                        .message("Profile retrieved successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PutMapping
    @Operation(summary = "Update authenticated user profile")
    public ResponseEntity<ApiResponse<ProfileResponseDto>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDto request,
            HttpServletRequest httpRequest
    ) {
        ProfileResponseDto data = profileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.<ProfileResponseDto>builder()
                        .success(true)
                        .message("Profile updated successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}



