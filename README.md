# WealthWise Backend

User Authentication and User Management module for Spring Boot 4 + Java 17.

## What is included

- JWT-based auth (`/auth/signup`, `/auth/signin`, protected endpoints)
- User profile management (`/auth/me`, `/auth/profile`, `/auth/change-password`, `/auth/account`)
- Password reset flow (`/auth/forgot-password`, `/auth/reset-password`)
- BCrypt password hashing
- UUID primary keys and PostgreSQL-ready JPA mappings

## Configuration

Set these values in `src/main/resources/application.properties` or environment variables:

- `security.jwt.secret` (must be a strong secret)
- `security.jwt.expiration-ms` (default `86400000`)

## Quick run

```bash
./mvnw clean test
./mvnw spring-boot:run
```

## Auth header format

Use JWT in request headers for protected routes:

```text
Authorization: Bearer <jwt-token>
```

