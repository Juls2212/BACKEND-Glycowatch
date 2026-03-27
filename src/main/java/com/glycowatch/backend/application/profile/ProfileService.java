package com.glycowatch.backend.application.profile;

import com.glycowatch.backend.interfaces.dto.profile.ProfileResponseDto;
import com.glycowatch.backend.interfaces.dto.profile.UpdateProfileRequestDto;

public interface ProfileService {

    ProfileResponseDto getProfile(String authenticatedEmail);

    ProfileResponseDto updateProfile(String authenticatedEmail, UpdateProfileRequestDto request);
}

