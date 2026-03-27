package com.glycowatch.backend.application.auth;

import com.glycowatch.backend.interfaces.dto.auth.LoginRequestDto;
import com.glycowatch.backend.interfaces.dto.auth.LoginResponseDto;
import com.glycowatch.backend.interfaces.dto.auth.RefreshTokenRequestDto;
import com.glycowatch.backend.interfaces.dto.auth.RefreshTokenResponseDto;
import com.glycowatch.backend.interfaces.dto.auth.RegisterRequestDto;
import com.glycowatch.backend.interfaces.dto.auth.RegisterResponseDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request);
}

