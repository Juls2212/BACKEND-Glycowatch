package com.glycowatch.measurement.service;

import com.glycowatch.measurement.dto.IngestMeasurementRequestDto;
import com.glycowatch.measurement.dto.IngestMeasurementResponseDto;

public interface MeasurementIngestionService {

    IngestMeasurementResponseDto ingestMeasurement(
            String deviceIdentifierHeader,
            String deviceKeyHeader,
            IngestMeasurementRequestDto request
    );
}



