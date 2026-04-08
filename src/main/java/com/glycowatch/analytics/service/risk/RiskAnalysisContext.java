package com.glycowatch.analytics.service.risk;

import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.profile.model.UserProfileEntity;
import java.util.List;

public record RiskAnalysisContext(
        UserProfileEntity profile,
        List<GlucoseMeasurementEntity> recentMeasurementsDesc,
        long recentAlertsCount
) {
}



