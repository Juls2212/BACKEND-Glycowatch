package com.glycowatch.profile.service;

import com.glycowatch.profile.dto.ProfileResponseDto;
import com.glycowatch.profile.dto.UpdateProfileRequestDto;

public interface ProfileService {

    ProfileResponseDto getProfile(String authenticatedEmail);

    ProfileResponseDto updateProfile(String authenticatedEmail, UpdateProfileRequestDto request);
}



