package com.glycowatch.backend.application.alert;

import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.interfaces.dto.alert.AlertResponseDto;
import java.util.List;

public interface AlertService {

    void generateForMeasurement(GlucoseMeasurementEntity measurement);

    List<AlertResponseDto> getAlerts(String authenticatedEmail);

    AlertResponseDto markAsRead(String authenticatedEmail, Long alertId);
}

