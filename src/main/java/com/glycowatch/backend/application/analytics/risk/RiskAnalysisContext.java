package com.glycowatch.backend.application.analytics.risk;

import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.domain.user.UserProfileEntity;
import java.util.List;

public record RiskAnalysisContext(
        UserProfileEntity profile,
        List<GlucoseMeasurementEntity> recentMeasurementsDesc,
        long recentAlertsCount
) {
}

