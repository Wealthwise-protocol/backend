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
| GET | `/funds` | ✅ | List/search funds with pagination |
| POST | `/funds` | ✅ | Get fund details by id |
| GET | `/funds/{id}/nav-history` | ✅ | Get fund NAV history |
| POST | `/funds/{id}/invest` | ✅ | Invest in a fund |

### Funds API Pagination

`GET /funds` supports pagination and filtering with query params:

- `page` (default `0`, must be `>= 0`)
- `size` (default `10`, range `1-100`)
- `search` (optional text search by fund name)
- `category` (optional exact category filter)

Example:

```bash
curl -X GET "http://localhost:9095/funds?page=0&size=10&search=axis&category=Equity" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

Response uses Spring Page format and includes:

- `content`
- `totalElements`
- `totalPages`
- `number`
- `size`

`/funds` endpoints now require a valid JWT token. Requests without token return `401 Unauthorized`.

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
