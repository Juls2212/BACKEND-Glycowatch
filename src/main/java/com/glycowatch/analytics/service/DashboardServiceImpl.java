package com.glycowatch.analytics.service;

import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.auth.model.UserEntity;
import com.glycowatch.alert.repository.AlertRepository;
import com.glycowatch.measurement.repository.GlucoseMeasurementRepository;
import com.glycowatch.auth.repository.UserRepository;
import com.glycowatch.analytics.dto.ChartPointDto;
import com.glycowatch.analytics.dto.DashboardResponseDto;
import com.glycowatch.common.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        Long userId = user.getId();

        GlucoseMeasurementEntity latest = glucoseMeasurementRepository
                .findFirstByUserIdAndIsValidTrueOrderByMeasuredAtDesc(userId)
                .orElse(null);

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<GlucoseMeasurementEntity> recentMeasurements =
                glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqual(
                        userId,
                        since,
                        PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "measuredAt"))
                ).getContent();

        RecentStats stats = calculateRecentStats(recentMeasurements);
        long alertsCount = alertRepository.countByUserId(userId);

        DashboardResponseDto.LatestMeasurementDto latestDto = latest == null
                ? null
                : new DashboardResponseDto.LatestMeasurementDto(
                        latest.getGlucoseValue(),
                        latest.getUnit(),
                        latest.getMeasuredAt()
                );

        return new DashboardResponseDto(
                latestDto,
                stats.average(),
                stats.min(),
                stats.max(),
                alertsCount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartPointDto> getChartData(String authenticatedEmail, LocalDate from, LocalDate to) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException("INVALID_DATE_RANGE", "'from' must be earlier than or equal to 'to'.", HttpStatus.BAD_REQUEST);
        }

        return queryChartMeasurements(user.getId(), from, to).stream()
                .map(measurement -> new ChartPointDto(measurement.getMeasuredAt(), measurement.getGlucoseValue()))
                .toList();
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private RecentStats calculateRecentStats(List<GlucoseMeasurementEntity> recentMeasurements) {
        if (recentMeasurements == null || recentMeasurements.isEmpty()) {
            return new RecentStats(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal sum = recentMeasurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(
                BigDecimal.valueOf(recentMeasurements.size()),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal min = recentMeasurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal max = recentMeasurements.stream()
                .map(GlucoseMeasurementEntity::getGlucoseValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new RecentStats(average, min, max);
    }

    private record RecentStats(BigDecimal average, BigDecimal min, BigDecimal max) {
    }

    private List<GlucoseMeasurementEntity> queryChartMeasurements(Long userId, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return glucoseMeasurementRepository.findTop20ByUserIdAndIsValidTrueOrderByMeasuredAtDesc(userId).stream()
                    .sorted((left, right) -> left.getMeasuredAt().compareTo(right.getMeasuredAt()))
                    .toList();
        }
        if (from != null && to == null) {
            Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
            return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
                    userId,
                    fromInstant
            );
        }
        if (from == null) {
            Instant toInstant = to.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
            return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtLessThanEqualOrderByMeasuredAtAsc(
                    userId,
                    toInstant
            );
        }

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
        return glucoseMeasurementRepository.findByUserIdAndIsValidTrueAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                userId,
                fromInstant,
                toInstant
        );
    }
}


