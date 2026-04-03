package com.glycowatch.backend.profile.service;

import com.glycowatch.backend.profile.dto.ProfileResponseDto;
import com.glycowatch.backend.profile.dto.UpdateProfileRequestDto;

public interface ProfileService {

    ProfileResponseDto getProfile(String authenticatedEmail);

    ProfileResponseDto updateProfile(String authenticatedEmail, UpdateProfileRequestDto request);
}


