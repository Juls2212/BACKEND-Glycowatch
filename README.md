GlycoWatch Backend
 Overview

GlycoWatch is a web platform designed to support people with diabetes in managing their glucose levels.

This backend provides the core services for:

User authentication and authorization (JWT)
Glucose measurements management
Profile management
Alerts and notifications
Analytics and trends

Future extensions include:

AI-powered analysis (Gemini integration)
IoT integration via ESP32 devices

Architecture

The backend follows a modular monolith approach, organized by business capability:

auth → authentication and authorization
profile → user profile management
measurement → glucose data handling
analytics → trends and risk analysis
alert → alert logic
device → device integration (future ESP32)
common → shared utilities and exceptions
security → JWT and Spring Security config

This structure avoids overengineering while remaining scalable and easy to maintain.

⚙️ Tech Stack
Java 21
Spring Boot
Spring Security (JWT)
PostgreSQL
Maven
Flyway (database migrations)
 Running the project locally
1. Set environment variables
$env:JWT_SECRET="your-very-secure-secret-32+chars"
$env:DB_PASSWORD="your-db-password"
2. Run backend
mvn spring-boot:run
 API Documentation (Swagger)

Once the backend is running:

Swagger UI:
👉 http://localhost:8081/swagger-ui.html
OpenAPI JSON:
👉 http://localhost:8081/v3/api-docs
🔐 Environment Variables
Variable	Description
JWT_SECRET	Secret key for JWT signing (min 32 chars)
DB_HOST	Database host
DB_PORT	Database port (default 5432)
DB_NAME	Database name
DB_USER	Database user
DB_PASSWORD	Database password
DB_SSL_MODE	SSL mode (disable/local, require/prod)
🧪 Main Endpoints
Auth
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
Profile
GET /api/v1/profile
PUT /api/v1/profile
Measurements
POST /api/v1/measurements
GET /api/v1/measurements
GET /api/v1/measurements/latest
Analytics
GET /api/v1/analytics/dashboard
GET /api/v1/analytics/risk
Devices (future-ready)
POST /api/v1/iot/*
🧱 Database Strategy
Flyway is used as the single source of truth

Hibernate uses:

ddl-auto: validate
No automatic schema changes in runtime
⚠️ Security Notes
No secrets are stored in the repository
Local configs are handled via environment variables
Sensitive files are ignored via .gitignore