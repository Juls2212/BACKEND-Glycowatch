package com.glycowatch.auth.service;

import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.profile.model.UserProfileEntity;
import com.glycowatch.auth.model.UserRole;
import com.glycowatch.profile.repository.UserProfileRepository;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.security.JwtTokenProvider;
import com.glycowatch.security.TokenType;
import com.glycowatch.auth.dto.AuthTokensDto;
import com.glycowatch.auth.dto.LoginRequestDto;
import com.glycowatch.auth.dto.LoginResponseDto;
import com.glycowatch.auth.dto.RefreshTokenRequestDto;
import com.glycowatch.auth.dto.RefreshTokenResponseDto;
import com.glycowatch.auth.dto.RegisterRequestDto;
import com.glycowatch.auth.dto.RegisterResponseDto;
import com.glycowatch.auth.dto.UserSummaryDto;
import com.glycowatch.common.exception.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BigDecimal DEFAULT_HYPOGLYCEMIA_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal DEFAULT_HYPERGLYCEMIA_THRESHOLD = new BigDecimal("180");
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", "Email is already registered.", HttpStatus.CONFLICT);
        }

        Instant now = Instant.now();
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .active(Boolean.TRUE)
                .createdAt(now)
                .createdBy(SYSTEM_ACTOR)
                .build();

        UserEntity savedUser = userRepository.save(user);

        UserProfileEntity profile = UserProfileEntity.builder()
                .user(savedUser)
                .fullName(request.fullName().trim())
                .hypoglycemiaThreshold(DEFAULT_HYPOGLYCEMIA_THRESHOLD)
                .hyperglycemiaThreshold(DEFAULT_HYPERGLYCEMIA_THRESHOLD)
                .timezone(DEFAULT_TIMEZONE)
                .createdAt(now)
                .createdBy(SYSTEM_ACTOR)
                .build();
        userProfileRepository.save(profile);

        return new RegisterResponseDto(savedUser.getId(), savedUser.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new ApiException("INVALID_CREDENTIALS", "Invalid email or password.", HttpStatus.UNAUTHORIZED);
        } catch (AuthenticationException ex) {
            throw new ApiException("AUTHENTICATION_FAILED", "Authentication failed.", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "User account is inactive.", HttpStatus.UNAUTHORIZED));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new LoginResponseDto(
                new AuthTokensDto(accessToken, refreshToken),
                new UserSummaryDto(user.getId(), user.getEmail(), user.getRole())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        String refreshToken = request.refreshToken();
        if (!jwtTokenProvider.isTokenValid(refreshToken, TokenType.REFRESH)) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired.", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtTokenProvider.extractSubject(refreshToken);
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "User account is inactive.", HttpStatus.UNAUTHORIZED));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        return new RefreshTokenResponseDto(newAccessToken);
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail == null ? null : rawEmail.trim().toLowerCase();
    }
}


