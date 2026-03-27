CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    measurement_id BIGINT NOT NULL REFERENCES glucose_measurements(id),
    type VARCHAR(30) NOT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(255) NULL
);

CREATE INDEX IF NOT EXISTS idx_alerts_user_created_at ON alerts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_measurement_id ON alerts(measurement_id);

