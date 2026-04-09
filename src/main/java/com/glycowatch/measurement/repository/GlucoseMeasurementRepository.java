package com.glycowatch.measurement.repository;

import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlucoseMeasurementRepository extends JpaRepository<GlucoseMeasurementEntity, Long> {

    boolean existsByDeduplicationHash(String deduplicationHash);

    Page<GlucoseMeasurementEntity> findByUserIdAndIsValidTrue(Long userId, Pageable pageable);

    Page<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqual(
            Long userId,
            Instant from,
            Pageable pageable
    );

    Page<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtLessThanEqual(
            Long userId,
            Instant to,
            Pageable pageable
    );

    Page<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtBetween(
            Long userId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    List<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
            Long userId,
            Instant from
    );

    List<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtLessThanEqualOrderByMeasuredAtAsc(
            Long userId,
            Instant to
    );

    List<GlucoseMeasurementEntity> findByUserIdAndIsValidTrueAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            Long userId,
            Instant from,
            Instant to
    );

    Optional<GlucoseMeasurementEntity> findFirstByUserIdAndIsValidTrueOrderByMeasuredAtDesc(Long userId);

    Optional<GlucoseMeasurementEntity> findByIdAndUserId(Long id, Long userId);

    List<GlucoseMeasurementEntity> findTop20ByUserIdAndIsValidTrueOrderByMeasuredAtDesc(Long userId);

    @Query("""
            SELECT AVG(m.glucoseValue), MIN(m.glucoseValue), MAX(m.glucoseValue)
            FROM GlucoseMeasurementEntity m
            WHERE m.user.id = :userId
              AND m.isValid = true
              AND m.measuredAt >= :since
            """)
    Object[] getRecentStats(@Param("userId") Long userId, @Param("since") Instant since);
}


