# Environment Variables

## Required for all environments

- `JWT_SECRET`: JWT signing secret with at least 32 characters.

## Required for production

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

## Optional for production

- `DB_PORT` defaults to `5432`
- `DB_SSL_MODE` defaults to `require`
- `SERVER_PORT` defaults to `8081`
- `PORT` overrides `SERVER_PORT`
- `JWT_ACCESS_EXPIRATION_MINUTES` defaults to `15`
- `JWT_REFRESH_EXPIRATION_DAYS` defaults to `7`
- `JWT_ISSUER` defaults to `glycowatch-backend`
- `CORS_ALLOWED_ORIGINS` defaults to `http://localhost:3000`

## Local development defaults

If `SPRING_PROFILES_ACTIVE` is not set, Spring uses the `local` profile.

Local defaults:

- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=glycowatch`
- `DB_USER=postgres`
- `DB_PASSWORD=` empty by default
- `DB_SSL_MODE=disable`

You still must provide `JWT_SECRET` locally.

## Notes

- Local PostgreSQL SSL is disabled by default to avoid the startup error `This server does not support SSL`.
- Production keeps SSL enabled by default through `DB_SSL_MODE=require`.
- Flyway is the only schema migration mechanism. Hibernate validates the schema but does not mutate it.
