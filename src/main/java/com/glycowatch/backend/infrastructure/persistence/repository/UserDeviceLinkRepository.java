package com.glycowatch.backend.infrastructure.persistence.repository;

import com.glycowatch.backend.domain.device.UserDeviceLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceLinkRepository extends JpaRepository<UserDeviceLinkEntity, Long> {

    List<UserDeviceLinkEntity> findByUserIdAndActiveTrue(Long userId);

    Optional<UserDeviceLinkEntity> findByUserIdAndDeviceIdAndActiveTrue(Long userId, Long deviceId);

    Optional<UserDeviceLinkEntity> findTopByUserIdAndDeviceIdOrderByLinkedAtDesc(Long userId, Long deviceId);

    Optional<UserDeviceLinkEntity> findTopByDeviceIdAndActiveTrueOrderByLinkedAtDesc(Long deviceId);
}
