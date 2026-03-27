# Authentication API Examples

## Register

### Request
`POST /api/v1/auth/register`

```json
{
  "email": "user@example.com",
  "password": "StrongPass123",
  "fullName": "Juan Perez"
}
```

### Response (200)
```json
{
  "success": true,
  "message": "User registered successfully.",
  "data": {
    "userId": 1,
    "email": "user@example.com"
  },
  "timestamp": "2026-03-21T12:00:00Z",
  "path": "/api/v1/auth/register"
}
```

## Login

### Request
`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "StrongPass123"
}
```

### Response (200)
```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "tokens": {
      "accessToken": "jwt_access_token",
      "refreshToken": "jwt_refresh_token"
    },
    "user": {
      "id": 1,
      "email": "user@example.com",
      "role": "USER"
    }
  },
  "timestamp": "2026-03-21T12:00:00Z",
  "path": "/api/v1/auth/login"
}
```

## Refresh Token

### Request
`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "jwt_refresh_token"
}
```

### Response (200)
```json
{
  "success": true,
  "message": "Token refreshed successfully.",
  "data": {
    "accessToken": "new_jwt_access_token"
  },
  "timestamp": "2026-03-21T12:00:00Z",
  "path": "/api/v1/auth/refresh"
}
```

