package com.glycowatch.measurement.service;

import com.glycowatch.measurement.dto.IngestMeasurementResponseDto;
import com.glycowatch.measurement.dto.ManualMeasurementRequestDto;

public interface MeasurementManualService {

    IngestMeasurementResponseDto createManualMeasurement(String authenticatedEmail, ManualMeasurementRequestDto request);
}



