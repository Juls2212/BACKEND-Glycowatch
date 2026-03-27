package com.glycowatch.backend.application.analytics;

import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.domain.user.UserEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.AlertRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserRepository;
import com.glycowatch.backend.interfaces.dto.analytics.ChartPointDto;
import com.glycowatch.backend.interfaces.dto.analytics.DashboardResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    public List<ChartPointDto> getChartData(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);

        return glucoseMeasurementRepository.findByUserIdAndIsValidTrue(
                        user.getId(),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "measuredAt"))
                ).getContent().stream()
                .map(measurement -> new ChartPointDto(measurement.getMeasuredAt(), measurement.getGlucoseValue()))
                .sorted(Comparator.comparing(ChartPointDto::measuredAt))
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
}

