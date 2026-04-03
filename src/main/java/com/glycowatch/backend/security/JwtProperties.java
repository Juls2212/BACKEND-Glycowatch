package com.glycowatch.backend.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secret is required.")
        @Size(min = 32, message = "JWT secret must be at least 32 characters long.")
        String secret,
        @Min(value = 1, message = "Access token expiration must be greater than zero.")
        long accessTokenExpirationMinutes,
        @Min(value = 1, message = "Refresh token expiration must be greater than zero.")
        long refreshTokenExpirationDays,
        @NotBlank(message = "JWT issuer is required.")
        String issuer
) {
}


