CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    unique_identifier VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,
    auth_key_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS user_device_link (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    device_id BIGINT NOT NULL REFERENCES devices(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    linked_at TIMESTAMP NOT NULL,
    unlinked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(255) NULL
);

CREATE INDEX IF NOT EXISTS idx_devices_identifier ON devices(unique_identifier);
CREATE INDEX IF NOT EXISTS idx_user_device_link_user_active ON user_device_link(user_id, active);
CREATE INDEX IF NOT EXISTS idx_user_device_link_device_active ON user_device_link(device_id, active);

