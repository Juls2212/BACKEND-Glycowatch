package com.glycowatch.backend.infrastructure.persistence.repository;

import com.glycowatch.backend.domain.alert.AlertEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    List<AlertEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AlertEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant since);

    long deleteByMeasurementId(Long measurementId);
}
