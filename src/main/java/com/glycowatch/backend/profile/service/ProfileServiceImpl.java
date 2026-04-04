package com.glycowatch.backend.profile.service;

import com.glycowatch.backend.auth.model.UserEntity;
import com.glycowatch.backend.profile.model.UserProfileEntity;
import com.glycowatch.backend.profile.repository.UserProfileRepository;
import com.glycowatch.backend.auth.repository.UserRepository;
import com.glycowatch.backend.profile.dto.ProfileResponseDto;
import com.glycowatch.backend.profile.dto.UpdateProfileRequestDto;
import com.glycowatch.backend.common.exception.ApiException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("PROFILE_NOT_FOUND", "User profile was not found.", HttpStatus.NOT_FOUND));

        return toProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public ProfileResponseDto updateProfile(String authenticatedEmail, UpdateProfileRequestDto request) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("PROFILE_NOT_FOUND", "User profile was not found.", HttpStatus.NOT_FOUND));

        profile.setFullName(trimToNull(request.fullName()));
        profile.setBirthDate(request.birthDate());
        profile.setHypoglycemiaThreshold(request.hypoglycemiaThreshold());
        profile.setHyperglycemiaThreshold(request.hyperglycemiaThreshold());
        profile.setTimezone(normalizeTimezone(request.timezone()));
        profile.setWeightKg(request.weightKg());
        profile.setHeightCm(request.heightCm());
        profile.setUpdatedAt(Instant.now());
        profile.setUpdatedBy(user.getEmail());

        UserProfileEntity updatedProfile = userProfileRepository.save(profile);
        return toProfileResponse(user, updatedProfile);
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private ProfileResponseDto toProfileResponse(UserEntity user, UserProfileEntity profile) {
        return new ProfileResponseDto(
                user.getId(),
                user.getEmail(),
                profile.getFullName(),
                profile.getBirthDate(),
                profile.getHypoglycemiaThreshold(),
                profile.getHyperglycemiaThreshold(),
                profile.getTimezone(),
                profile.getWeightKg(),
                profile.getHeightCm()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeTimezone(String timezone) {
        String normalized = trimToNull(timezone);
        if (normalized == null) {
            return null;
        }
        try {
            return ZoneId.of(normalized).getId();
        } catch (DateTimeException ex) {
            throw new ApiException("INVALID_TIMEZONE", "Timezone is invalid.", HttpStatus.BAD_REQUEST);
        }
    }
}

