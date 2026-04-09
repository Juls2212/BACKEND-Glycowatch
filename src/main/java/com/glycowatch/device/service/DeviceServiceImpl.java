package com.glycowatch.device.service;

import com.glycowatch.device.model.DeviceEntity;
import com.glycowatch.device.model.DeviceStatus;
import com.glycowatch.device.model.UserDeviceLinkEntity;
import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.device.repository.DeviceRepository;
import com.glycowatch.device.repository.UserDeviceLinkRepository;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.device.dto.CreateDeviceRequestDto;
import com.glycowatch.device.dto.CreateDeviceResponseDto;
import com.glycowatch.device.dto.DeviceResponseDto;
import com.glycowatch.device.dto.LinkDeviceResponseDto;
import com.glycowatch.device.dto.RemoveDeviceResponseDto;
import com.glycowatch.device.dto.ToggleDeviceResponseDto;
import com.glycowatch.common.exception.ApiException;
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

    @Override
    @Transactional
    public RemoveDeviceResponseDto removeDevice(String authenticatedEmail, Long deviceId) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_FOUND", "Device was not found.", HttpStatus.NOT_FOUND));

        UserDeviceLinkEntity activeLink = userDeviceLinkRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceId)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_LINKED", "Device is not linked to the authenticated user.", HttpStatus.FORBIDDEN));

        Instant now = Instant.now();
        activeLink.setActive(Boolean.FALSE);
        activeLink.setUnlinkedAt(now);
        activeLink.setUpdatedAt(now);
        activeLink.setUpdatedBy(user.getEmail());
        userDeviceLinkRepository.save(activeLink);

        device.setActive(Boolean.FALSE);
        device.setStatus(DeviceStatus.DISABLED);
        device.setUpdatedAt(now);
        device.setUpdatedBy(user.getEmail());
        DeviceEntity updatedDevice = deviceRepository.save(device);

        return new RemoveDeviceResponseDto(
                updatedDevice.getId(),
                true,
                updatedDevice.getStatus(),
                now
        );
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




