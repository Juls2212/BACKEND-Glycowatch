package com.glycowatch.backend.measurement.service;

import com.glycowatch.backend.measurement.dto.IngestMeasurementRequestDto;
import com.glycowatch.backend.measurement.dto.IngestMeasurementResponseDto;

public interface MeasurementIngestionService {

    IngestMeasurementResponseDto ingestMeasurement(
            String deviceIdentifierHeader,
            String deviceKeyHeader,
            IngestMeasurementRequestDto request
    );
}


