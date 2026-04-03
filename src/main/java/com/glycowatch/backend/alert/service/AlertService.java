package com.glycowatch.backend.alert.service;

import com.glycowatch.backend.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.backend.alert.dto.AlertResponseDto;
import java.util.List;

public interface AlertService {

    void generateForMeasurement(GlucoseMeasurementEntity measurement);

    List<AlertResponseDto> getAlerts(String authenticatedEmail);

    AlertResponseDto markAsRead(String authenticatedEmail, Long alertId);
}


