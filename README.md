# WealthWise SIP Module Backend

Spring Boot 4 + Java 17 REST API for the SIP (Systematic Investment Plan) module of the WealthWise Investment Management System.

## Overview

This module handles all SIP-related operations including creating, managing, and tracking Systematic Investment Plans for users. It provides RESTful endpoints for SIP CRUD operations, installment tracking, and investment analytics.

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

- **SIP Management**: Create, read, update, and delete SIPs
- **Installment Tracking**: Track monthly investments and payments
- **Portfolio Analytics**: Monitor total invested amount and current value
- **User Authentication**: JWT-based authentication for secure access
- **Email Notifications**: Automated email alerts for SIP activities

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

### SIP Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/sips` | ✅ | Get user's SIPs |
| POST | `/sips` | ✅ | Create new SIP |
| PATCH | `/sips` | ✅ | Update existing SIP |
| DELETE | `/sips?id={sipId}` | ✅ | Delete SIP |

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
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers (AuthController, SipController)
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities (User, Sip, SipInstallment)
│   │   ├── repository/     # JPA repositories
│   │   ├── security/       # JWT security configuration
│   │   └── service/        # Business logic services
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/wealthwise/
        └── # Test classes
```

## Contributing

1. This module is part of the WealthWise backend monorepo
2. Follow the branch strategy: feature branches for new features
3. Ensure all tests pass before submitting PR
4. Update documentation for any API changes
