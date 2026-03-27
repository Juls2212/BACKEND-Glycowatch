CREATE TABLE IF NOT EXISTS glucose_measurements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    device_id BIGINT NOT NULL REFERENCES devices(id),
    glucose_value DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    measured_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    invalid_reason VARCHAR(255) NULL,
    deduplication_hash VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_glucose_measurements_user_measured_at
    ON glucose_measurements(user_id, measured_at DESC);
CREATE INDEX IF NOT EXISTS idx_glucose_measurements_device_measured_at
    ON glucose_measurements(device_id, measured_at DESC);

