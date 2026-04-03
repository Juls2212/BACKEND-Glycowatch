package com.glycowatch.backend.auth.service;

import com.glycowatch.backend.auth.dto.LoginRequestDto;
import com.glycowatch.backend.auth.dto.LoginResponseDto;
import com.glycowatch.backend.auth.dto.RefreshTokenRequestDto;
import com.glycowatch.backend.auth.dto.RefreshTokenResponseDto;
import com.glycowatch.backend.auth.dto.RegisterRequestDto;
import com.glycowatch.backend.auth.dto.RegisterResponseDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request);
}


