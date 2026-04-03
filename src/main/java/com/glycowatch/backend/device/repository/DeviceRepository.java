package com.glycowatch.backend.device.repository;

import com.glycowatch.backend.device.model.DeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    boolean existsByUniqueIdentifierIgnoreCase(String uniqueIdentifier);

    Optional<DeviceEntity> findByIdAndActiveTrue(Long id);

    Optional<DeviceEntity> findByUniqueIdentifierIgnoreCaseAndActiveTrue(String uniqueIdentifier);
}

