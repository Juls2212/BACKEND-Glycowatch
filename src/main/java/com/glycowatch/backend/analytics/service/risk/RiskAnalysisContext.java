package com.glycowatch.backend.analytics.service.risk;

import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.profile.model.UserProfileEntity;
import java.util.List;

public record RiskAnalysisContext(
        UserProfileEntity profile,
        List<GlucoseMeasurementEntity> recentMeasurementsDesc,
        long recentAlertsCount
) {
}


