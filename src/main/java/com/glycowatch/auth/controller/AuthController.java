package com.glycowatch.auth.controller;

import com.glycowatch.auth.service.AuthService;
import com.glycowatch.auth.dto.LoginRequestDto;
import com.glycowatch.auth.dto.LoginResponseDto;
import com.glycowatch.auth.dto.RefreshTokenRequestDto;
import com.glycowatch.auth.dto.RefreshTokenResponseDto;
import com.glycowatch.auth.dto.RegisterRequestDto;
import com.glycowatch.auth.dto.RegisterResponseDto;
import com.glycowatch.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest
    ) {
        RegisterResponseDto data = authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.<RegisterResponseDto>builder()
                        .success(true)
                        .message("User registered successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and issue access/refresh tokens")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        LoginResponseDto data = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.<LoginResponseDto>builder()
                        .success(true)
                        .message("Login successful.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Issue a new access token using refresh token")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request,
            HttpServletRequest httpRequest
    ) {
        RefreshTokenResponseDto data = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.<RefreshTokenResponseDto>builder()
                        .success(true)
                        .message("Token refreshed successfully.")
                        .data(data)
                        .timestamp(Instant.now())
                        .path(httpRequest.getRequestURI())
                        .build()
        );
    }
}



