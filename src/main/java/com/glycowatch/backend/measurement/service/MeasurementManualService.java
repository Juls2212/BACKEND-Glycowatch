package com.glycowatch.backend.measurement.service;

import com.glycowatch.backend.measurement.dto.IngestMeasurementResponseDto;
import com.glycowatch.backend.measurement.dto.ManualMeasurementRequestDto;

public interface MeasurementManualService {

    IngestMeasurementResponseDto createManualMeasurement(String authenticatedEmail, ManualMeasurementRequestDto request);
}


