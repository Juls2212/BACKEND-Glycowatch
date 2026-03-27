CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    full_name VARCHAR(255),
    birth_date DATE NULL,
    hypoglycemia_threshold DECIMAL(10,2) NOT NULL,
    hyperglycemia_threshold DECIMAL(10,2) NOT NULL,
    timezone VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(255) NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

