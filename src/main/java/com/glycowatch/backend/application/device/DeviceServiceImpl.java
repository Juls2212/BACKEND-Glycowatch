package com.glycowatch.backend.application.device;

import com.glycowatch.backend.domain.device.DeviceEntity;
import com.glycowatch.backend.domain.device.DeviceStatus;
import com.glycowatch.backend.domain.device.UserDeviceLinkEntity;
import com.glycowatch.backend.domain.user.UserEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.DeviceRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserDeviceLinkRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserRepository;
import com.glycowatch.backend.interfaces.dto.device.CreateDeviceRequestDto;
import com.glycowatch.backend.interfaces.dto.device.CreateDeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.DeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.LinkDeviceResponseDto;
import com.glycowatch.backend.interfaces.dto.device.ToggleDeviceResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserDeviceLinkRepository userDeviceLinkRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> listDevices(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        return userDeviceLinkRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                .map(UserDeviceLinkEntity::getDevice)
                .sorted(Comparator.comparing(DeviceEntity::getCreatedAt).reversed())
                .map(this::toDeviceResponse)
                .toList();
    }

    @Override
    @Transactional
    public CreateDeviceResponseDto registerDevice(String authenticatedEmail, CreateDeviceRequestDto request) {
        String identifier = request.identifier().trim();
        if (deviceRepository.existsByUniqueIdentifierIgnoreCase(identifier)) {
            throw new ApiException("DEVICE_IDENTIFIER_ALREADY_EXISTS", "Device identifier is already registered.", HttpStatus.CONFLICT);
        }

        String apiKey = generateApiKey();
        Instant now = Instant.now();

        DeviceEntity device = DeviceEntity.builder()
                .name(request.name().trim())
                .uniqueIdentifier(identifier)
                .status(DeviceStatus.REGISTERED)
                .active(Boolean.TRUE)
                .authKeyHash(passwordEncoder.encode(apiKey))
                .createdAt(now)
                .createdBy(resolveActiveUser(authenticatedEmail).getEmail())
                .build();

        DeviceEntity savedDevice = deviceRepository.save(device);
        return new CreateDeviceResponseDto(savedDevice.getId(), apiKey);
    }

    @Override
    @Transactional
    public LinkDeviceResponseDto linkDevice(String authenticatedEmail, Long deviceId) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        DeviceEntity device = resolveActiveDevice(deviceId);

        userDeviceLinkRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceId)
                .ifPresent(link -> {
                    throw new ApiException("DEVICE_ALREADY_LINKED", "Device is already linked to this user.", HttpStatus.CONFLICT);
                });

        Instant now = Instant.now();
        UserDeviceLinkEntity link = userDeviceLinkRepository
                .findTopByUserIdAndDeviceIdOrderByLinkedAtDesc(user.getId(), deviceId)
                .orElseGet(() -> UserDeviceLinkEntity.builder()
                        .user(user)
                        .device(device)
                        .createdAt(now)
                        .createdBy(user.getEmail())
                        .build());

        link.setActive(Boolean.TRUE);
        link.setLinkedAt(now);
        link.setUnlinkedAt(null);
        link.setUpdatedAt(now);
        link.setUpdatedBy(user.getEmail());

        UserDeviceLinkEntity savedLink = userDeviceLinkRepository.save(link);
        return new LinkDeviceResponseDto(savedLink.getDevice().getId(), true, savedLink.getLinkedAt());
    }

    @Override
    @Transactional
    public ToggleDeviceResponseDto toggleDevice(String authenticatedEmail, Long deviceId) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_FOUND", "Device was not found.", HttpStatus.NOT_FOUND));

        userDeviceLinkRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceId)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_LINKED", "Device is not linked to the authenticated user.", HttpStatus.FORBIDDEN));

        boolean nextActive = !Boolean.TRUE.equals(device.getActive());
        device.setActive(nextActive);
        device.setStatus(nextActive ? DeviceStatus.ACTIVE : DeviceStatus.DISABLED);
        device.setUpdatedAt(Instant.now());
        device.setUpdatedBy(user.getEmail());

        DeviceEntity updatedDevice = deviceRepository.save(device);
        return new ToggleDeviceResponseDto(updatedDevice.getId(), Boolean.TRUE.equals(updatedDevice.getActive()), updatedDevice.getStatus());
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private DeviceEntity resolveActiveDevice(Long deviceId) {
        return deviceRepository.findByIdAndActiveTrue(deviceId)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_FOUND", "Device was not found or inactive.", HttpStatus.NOT_FOUND));
    }

    private DeviceResponseDto toDeviceResponse(DeviceEntity device) {
        return new DeviceResponseDto(
                device.getId(),
                device.getName(),
                device.getUniqueIdentifier(),
                device.getStatus(),
                Boolean.TRUE.equals(device.getActive())
        );
    }

    private String generateApiKey() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
