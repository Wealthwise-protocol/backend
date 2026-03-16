# WealthWise Backend

Spring Boot 4 + Java 17 REST API for the WealthWise Investment Management System.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA + Hibernate
- PostgreSQL (Neon)
- Lombok
- BCrypt password hashing

## Modules

| Module | Branch | Status |
|--------|--------|--------|
| User Auth & Management | `feature/user-auth-module` | ✅ Done |
| Portfolio | `feature/portfolio-module` | ⏳ Pending |
| Investment | `feature/investment-module` | ⏳ Pending |
| Reports | `feature/reports-module` | ⏳ Pending |

## API Endpoints

Base URL (local): `http://localhost:9095`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | ❌ | Register new user |
| POST | `/auth/signin` | ❌ | Login |
| POST | `/auth/signout` | ✅ | Logout |
| GET | `/auth/me` | ✅ | Get current user |
| PATCH | `/auth/profile` | ✅ | Update profile |
| POST | `/auth/change-password` | ✅ | Change password |
| POST | `/auth/forgot-password` | ❌ | Request reset token |
| POST | `/auth/reset-password` | ❌ | Reset password |
| DELETE | `/auth/account` | ✅ | Delete account |

## Local Setup

### Prerequisites
- Java 17+
- Maven

### Run locally

```bash
git clone https://github.com/Wealthwise-protocol/backend.git
cd backend
git checkout feature/user-auth-module
./mvnw spring-boot:run
```

Server starts on `http://localhost:9095`

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PORT` | Server port (default `9095` local, `8080` on Render) |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (min 32 chars) |
| `JWT_EXPIRATION_MS` | Token expiry in ms (default `86400000` = 24h) |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |

## Deployment

Deployed on **Render** using Docker.  
See `render.yaml` for config.

## Branch Strategy

Each module lives in its own feature branch.  
Never push directly to `main`.  
Open a PR when your module is complete.
