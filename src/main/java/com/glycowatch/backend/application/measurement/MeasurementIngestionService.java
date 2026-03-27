package com.glycowatch.backend.application.measurement;

import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementRequestDto;
import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementResponseDto;

public interface MeasurementIngestionService {

    IngestMeasurementResponseDto ingestMeasurement(
            String deviceIdentifierHeader,
            String deviceKeyHeader,
            IngestMeasurementRequestDto request
    );
}

