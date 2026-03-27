package com.glycowatch.backend.infrastructure.persistence.repository;

import com.glycowatch.backend.domain.device.DeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    boolean existsByUniqueIdentifierIgnoreCase(String uniqueIdentifier);

    Optional<DeviceEntity> findByIdAndActiveTrue(Long id);

    Optional<DeviceEntity> findByUniqueIdentifierIgnoreCaseAndActiveTrue(String uniqueIdentifier);
}
