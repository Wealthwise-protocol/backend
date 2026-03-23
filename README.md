# WealthWise Backend (Merged Modules)

Spring Boot 4 + Java 17 REST API for the WealthWise Investment Management System, now containing merged modules: SIP (Module 1 & 3) and Fund/Bookmark (Module 2).

## Overview

This backend handles all investment-related operations including SIP management, fund tracking, and bookmarking. It provides RESTful endpoints for CRUD operations across all modules.

**Recent Update**: Merged with `krish module` to include Fund and Bookmark functionality alongside existing SIP features.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA + Hibernate
- PostgreSQL (Neon)
- Lombok
- BCrypt password hashing
- Spring Mail (for notifications)

## Features

- **SIP Management**: Create, read, update, and delete SIPs (Modules 1 & 3)
- **Installment Tracking**: Track monthly investments and payments
- **Fund Management**: Browse, invest in mutual funds (Module 2)
- **Bookmarking**: Save favorite funds and investments (Module 2)
- **Portfolio Analytics**: Monitor total invested amount and current value
- **User Authentication**: JWT-based authentication for secure access
- **Email Notifications**: Automated email alerts for activities

## API Endpoints

Base URL (local): `http://localhost:9095`

### Authentication Endpoints
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

### SIP Endpoints (Modules 1 & 3)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/sips` | ✅ | Get user's SIPs |
| POST | `/sips` | ✅ | Create new SIP |
| PATCH | `/sips` | ✅ | Update existing SIP |
| DELETE | `/sips?id={sipId}` | ✅ | Delete SIP |

### Fund Endpoints (Module 2)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/funds` | ✅ | Get available funds |
| GET | `/funds/{id}` | ✅ | Get fund details |
| POST | `/funds/invest` | ✅ | Invest in a fund |
| GET | `/funds/nav/history` | ✅ | Get NAV history |

### Bookmark Endpoints (Module 2)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/bookmarks` | ✅ | Get user's bookmarks |
| POST | `/bookmarks` | ✅ | Add bookmark |
| DELETE | `/bookmarks/{id}` | ✅ | Remove bookmark |

## Testing with Postman

### Setup
1. Start the server: `./mvnw spring-boot:run`
2. Open Postman and create a new collection: "WealthWise Backend"
3. Set environment variable: `baseUrl = http://localhost:9095`

### Authentication Flow
1. **Sign In**: `POST {{baseUrl}}/auth/signin`
   - Body (JSON): `{"username":"testuser","password":"testpass"}`
   - Save `token` from response
2. Set header for all requests: `Authorization: Bearer {{token}}`

### Test Sequence
1. **SIP Module**: Create, list, update, delete SIPs
2. **Fund Module**: Browse funds, invest, check NAV history
3. **Bookmark Module**: Add/remove bookmarks for funds
4. **Error Cases**: Test 400/401/404 responses

### Sample Requests
- Create SIP: `POST {{baseUrl}}/sips` with `CreateSipRequest` JSON
- Get Funds: `GET {{baseUrl}}/funds`
- Add Bookmark: `POST {{baseUrl}}/bookmarks` with fund ID

Run collection in Postman Runner for automated testing.

## Data Models

### SIP Entity
- `id`: UUID (Primary Key)
- `user`: User reference
- `fundName`: String (Mutual fund name)
- `monthlyAmt`: BigDecimal (Monthly investment amount)
- `startDate`: LocalDate (SIP start date)
- `nextDebit`: LocalDate (Next debit date)
- `totalInvested`: BigDecimal (Total amount invested)
- `currentValue`: BigDecimal (Current portfolio value)
- `status`: String (ACTIVE/PAUSED/CANCELLED)
- `installments`: List of SipInstallment

### SipInstallment Entity
- Tracks individual monthly investments
- Links to parent SIP
- Records payment dates and amounts

## Local Setup

### Prerequisites
- Java 17+
- Maven
- PostgreSQL database

### Environment Variables
Create a `.env` file or set environment variables:

```bash
PORT=9095
DB_URL=jdbc:postgresql://your-db-host:5432/wealthwise_db
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
JWT_SECRET=your-32-character-jwt-secret-key
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
EMAIL_ENABLED=true
FRONTEND_URL=http://localhost:3000
```

### Run locally

```bash
git clone https://github.com/Wealthwise-protocol/backend.git
cd backend/SIP\ module\ Backend
./mvnw spring-boot:run
```

Server starts on `http://localhost:9095`

## Database Schema

The module uses PostgreSQL with the following main tables:
- `users`: User accounts
- `sips`: SIP plans
- `sip_installments`: Monthly investment records
- `funds`: Mutual fund information (Module 2)
- `bookmarks`: User bookmarks for funds (Module 2)
- `fund_nav_history`: NAV history for funds (Module 2)
- `password_reset_tokens`: For password recovery

## Testing

Run tests with:
```bash
./mvnw test
```

## Deployment

Deployed on **Render** using Docker.  
See `render.yaml` for deployment configuration.

## Project Structure

```
src/
├── main/
│   ├── java/com/wealthwise/
│   │   ├── config/          # Configuration classes (DataSeeder, NavSyncJob)
│   │   ├── controller/      # REST controllers (AuthController, SipController, FundController, BookmarkController)
│   │   ├── dto/            # Data Transfer Objects (request/response for all modules)
│   │   ├── entity/         # JPA entities (User, Sip, SipInstallment, Fund, Bookmark, FundNavHistory)
│   │   ├── repository/     # JPA repositories (all modules)
│   │   ├── security/       # JWT security configuration
│   │   ├── service/        # Business logic services (SipService, FundService, MfApiService)
│   │   └── WealthwiseBackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/wealthwise/
        └── # Test classes (JwtServiceTest, ApplicationTests)
```

## Contributing

1. This module is part of the WealthWise backend monorepo
2. Follow the branch strategy: feature branches for new features
3. Ensure all tests pass before submitting PR
4. Update documentation for any API changes
