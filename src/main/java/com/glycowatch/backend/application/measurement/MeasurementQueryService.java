package com.glycowatch.backend.application.measurement;

import com.glycowatch.backend.interfaces.dto.measurement.LatestMeasurementResponseDto;
import com.glycowatch.backend.interfaces.dto.measurement.MeasurementPageResponseDto;
import java.time.LocalDate;

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
