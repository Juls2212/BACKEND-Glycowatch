package com.glycowatch.alert.service;

import com.glycowatch.measurement.model.GlucoseMeasurementEntity;
import com.glycowatch.alert.dto.AlertResponseDto;
import java.util.List;

public interface AlertService {

    void generateForMeasurement(GlucoseMeasurementEntity measurement);

    List<AlertResponseDto> getAlerts(String authenticatedEmail);

    AlertResponseDto markAsRead(String authenticatedEmail, Long alertId);
}



