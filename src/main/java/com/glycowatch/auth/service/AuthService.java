package com.glycowatch.auth.service;

import com.glycowatch.auth.dto.LoginRequestDto;
import com.glycowatch.auth.dto.LoginResponseDto;
import com.glycowatch.auth.dto.RefreshTokenRequestDto;
import com.glycowatch.auth.dto.RefreshTokenResponseDto;
import com.glycowatch.auth.dto.RegisterRequestDto;
import com.glycowatch.auth.dto.RegisterResponseDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request);
}



