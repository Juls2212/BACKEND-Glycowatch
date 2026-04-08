package com.glycowatch.measurement.service;

import com.glycowatch.measurement.dto.LatestMeasurementResponseDto;
import com.glycowatch.measurement.dto.MeasurementPageResponseDto;
import java.time.LocalDate;

// Service interface for querying glucose measurements with pagination and filtering
public interface MeasurementQueryService {

    MeasurementPageResponseDto getMeasurements(
            String authenticatedEmail,
            int page,
            int size,
            LocalDate from,
            LocalDate to
    );

    LatestMeasurementResponseDto getLatestMeasurement(String authenticatedEmail);

    void deleteMeasurement(String authenticatedEmail, Long measurementId);
}


