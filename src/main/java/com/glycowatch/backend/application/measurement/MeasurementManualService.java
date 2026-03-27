package com.glycowatch.backend.application.measurement;

import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementResponseDto;
import com.glycowatch.backend.interfaces.dto.measurement.ManualMeasurementRequestDto;

public interface MeasurementManualService {

    IngestMeasurementResponseDto createManualMeasurement(String authenticatedEmail, ManualMeasurementRequestDto request);
}

