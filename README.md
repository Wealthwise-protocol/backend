# WealthWise Backend

Spring Boot 4 + Java 17 REST API for the WealthWise Investment Management System.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA + Hibernate
- PostgreSQL (Neon Cloud) / H2 (Development)
- Lombok
- BCrypt password hashing

## Modules

| Module | Status | Description |
|--------|--------|-------------|
| User Auth & Management | ✅ Complete | JWT auth, user profiles, password management |
| Payment Processing | ✅ Complete | Payment creation, validation, status tracking |
| SIP (Systematic Investment Plans) | ✅ Complete | SIP creation with payment, monthly installments |
| Transactions | ✅ Complete | Transaction recording, COMPLETED/FAILED status |
| Portfolio | ✅ Complete | Holdings tracking, NAV updates, portfolio summary |
| Funds | ✅ Complete | Fund listing, NAV history, search/filter |
| Bookmarks | ✅ Complete | User fund bookmarks |

## API Endpoints

**Base URL (local):** `http://localhost:8080`  
**Database:** H2 (Development) | PostgreSQL Neon (Production)

### Authentication Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | ❌ | Register new user |
| POST | `/auth/signin` | ❌ | Login and get JWT token |
| POST | `/auth/signout` | ✅ | Logout |
| GET | `/auth/me` | ✅ | Get current user profile |
| PATCH | `/auth/profile` | ✅ | Update user profile |
| POST | `/auth/change-password` | ✅ | Change password |
| POST | `/auth/forgot-password` | ❌ | Request password reset |
| POST | `/auth/reset-password` | ❌ | Reset password with token |
| DELETE | `/auth/account` | ✅ | Delete user account |

### Fund Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/funds` | ✅ | List all funds with pagination |
| GET | `/funds/{id}` | ✅ | Get fund details |
| GET | `/funds/{id}/nav-history` | ✅ | Get fund NAV history |

### Payment Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/payments` | ✅ | Create new payment |
| GET | `/payments` | ✅ | Get all user payments |
| GET | `/payments/{paymentId}` | ✅ | Get payment details |
| GET | `/payments/by-status` | ✅ | Filter payments by status |
| POST | `/payments/process-sip-creation` | ✅ | Process payment for SIP creation |
| POST | `/payments/process-installment` | ✅ | Process monthly installment payment |

### SIP (Systematic Investment Plan) Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/sips` | ✅ | Create new SIP with payment |
| GET | `/sips` | ✅ | Get all user SIPs |
| GET | `/sips/{sipId}` | ✅ | Get SIP details |
| PATCH | `/sips` | ✅ | Update SIP (monthly amount, status) |
| DELETE | `/sips?id={sipId}` | ✅ | Delete SIP |

### Transaction Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/transactions` | ✅ | Get all transactions (with sort) |
| GET | `/transactions/by-fund` | ✅ | Get transactions for specific fund |

### Portfolio Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/portfolio/summary` | ✅ | Get portfolio summary with holdings |
| GET | `/portfolio/holdings` | ✅ | Get all holdings details |

### Bookmark Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/bookmarks` | ✅ | Add fund to bookmarks |
| GET | `/bookmarks` | ✅ | Get all bookmarked funds |
| DELETE | `/bookmarks` | ✅ | Remove fund from bookmarks |

## Payment Module Features

### Overview
The Payment Module integrates payment validation with SIP creation and monthly installments:
- ✅ **Payment Creation** - Create payments in PENDING status
- ✅ **Payment Validation** - Validate payment before SIP creation
- ✅ **SIP Integration** - SIP requires valid payment
- ✅ **Transaction Recording** - COMPLETED transactions on successful payment, FAILED on failure
- ✅ **Installment Management** - Auto-create monthly installments
- ✅ **Audit Trail** - Failed payments recorded for audit

### Complete User Flow

```
1. User Signup → Registration
2. User Signin → JWT Token obtained
3. Browse Funds → View available funds
4. Create Payment → PENDING payment record (₹5000)
5. Create SIP → Validate payment + Create SIP + Process payment → SUCCESS
6. COMPLETED Transaction → ₹5000 investment recorded
7. Portfolio Updated → Units allocated to fund
8. Monthly Installment → Next month debit scheduled
9. Process Installment → Payment processed → Transaction created
```

### Payment Status Flow

```
CREATE (PENDING)
    ↓
PROCESS → SUCCESS (SIP created, Transaction COMPLETED)
    ↓ (OR)
PROCESS → FAILED (Transaction FAILED, no units allocated)
```

### Transaction Status Types

| Status | Description | Units Allocated |
|--------|-------------|-----------------|
| COMPLETED | Payment successful, units allocated | ✅ Yes |
| FAILED | Payment failed, no units | ❌ No |
| PENDING | Future use | N/A |

## SIP Creation Requirements

Before creating an SIP, you MUST:

1. **Create Payment**
   ```bash
   POST /payments
   {
     "amount": 5000,
     "paymentMethod": "CREDIT_CARD",
     "description": "SIP Initial Investment"
   }
   ```

2. **Create SIP with Payment ID**
   ```bash
   POST /sips
   {
     "fundId": "fund-uuid",
     "monthlyAmt": 5000,           # Must match payment amount
     "paymentId": "payment-uuid"    # From payment creation
   }
   ```

3. **Payment amount MUST equal SIP monthly amount**
   - ❌ Payment: ₹3000, SIP: ₹5000 → ERROR
   - ✅ Payment: ₹5000, SIP: ₹5000 → SUCCESS

## Pagination

`GET /funds` supports pagination and filtering with query params:

- `page` (default `0`, must be `>= 0`)
- `size` (default `10`, range `1-100`)
- `search` (optional text search by fund name)
- `category` (optional exact category filter)

Example:

```bash
curl -X GET "http://localhost:8080/funds?page=0&size=10&search=hdfc&category=Equity" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

Response uses Spring Page format:
- `content` - Array of funds
- `totalElements` - Total fund count
- `totalPages` - Total pages
- `number` - Current page
- `size` - Page size

## Local Setup

### Prerequisites
- Java 17+
- Maven 3.6+

### Quick Start (H2 - Development)

```bash
git clone https://github.com/Wealthwise-protocol/backend.git
cd backend
git checkout feature/user-auth-module

# H2 is pre-configured
mvn clean install
mvn spring-boot:run
```

Server starts on `http://localhost:8080`

**Access H2 Console:**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:wealthwise_db`
- Username: `sa`
- Password: *(leave empty)*

### Setup with PostgreSQL (Local)

1. **Install PostgreSQL locally**
   ```bash
   # Windows: Download from postgresql.org
   # macOS: brew install postgresql
   # Linux: sudo apt-get install postgresql
   ```

2. **Create database**
   ```bash
   createdb wealthwise_dev
   ```

3. **Update application.properties**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/wealthwise_dev
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

4. **Run application**
   ```bash
   mvn spring-boot:run
   ```

### Setup with Neon PostgreSQL (Cloud)

1. **Create Neon account** - https://console.neon.tech
2. **Create PostgreSQL database** in Neon
3. **Get connection string** from Neon dashboard
4. **Execute migration script**
   ```bash
   psql -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
   psql -f PAYMENT_MODULE_MIGRATION.sql
   ```
5. **Update application.properties**
   ```properties
   spring.datasource.url=your_neon_connection_string
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

## Database Migration (Neon PostgreSQL)

**Execute the payment module migration:**

```bash
# Using Neon SQL Editor
1. Go to https://console.neon.tech/
2. Select your project
3. Click "SQL Editor"
4. Copy entire content of PAYMENT_MODULE_MIGRATION.sql
5. Paste and click "Execute"
```

Or use command line:
```bash
psql your_neon_connection_string -f PAYMENT_MODULE_MIGRATION.sql
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Server port |
| `DB_URL` | H2 In-Memory | PostgreSQL JDBC URL |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | `` | Database password |
| `JWT_SECRET` | Generated | JWT signing secret (min 32 chars) |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiry in ms (24 hours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed frontend origins |

### Example .env file
```env
PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/wealthwise_dev
DB_USERNAME=postgres
DB_PASSWORD=dev_password
JWT_SECRET=your_very_secret_key_at_least_32_characters_long
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Testing with Postman

### Setup Postman Environment

1. Create new environment in Postman
2. Add variables:
   ```
   base_url: http://localhost:8080
   auth_token: <JWT Token>
   user_id: <Your User UUID>
   fund_id: <Fund UUID>
   payment_id: <Payment UUID>
   sip_id: <SIP UUID>
   ```

3. Use variables in requests: `{{base_url}}`, `{{auth_token}}`

### Test Complete Flow

See **PAYMENT_MODULE_TESTING_GUIDE.md** for detailed step-by-step Postman testing with all requests and expected responses.

## Documentation

| Document | Purpose |
|----------|---------|
| `PAYMENT_MODULE_GUIDE.md` | Complete payment module API documentation |
| `PAYMENT_MODULE_TESTING_GUIDE.md` | Step-by-step Postman testing guide |
| `PAYMENT_MODULE_IMPLEMENTATION_SUMMARY.md` | Technical implementation details |
| `PAYMENT_MODULE_MIGRATION.sql` | Database migration script |

## Deployment

### Deploy to Render (PostgreSQL)

1. Update `render.yaml` with Neon credentials
2. Push to GitHub
3. Render auto-deploys on push to main

**Environment variables in Render:**
```
DB_URL=<neon_connection_string>
DB_USERNAME=<neon_username>
DB_PASSWORD=<neon_password>
JWT_SECRET=<your_secret_key>
```

## Error Handling

### Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| `401 Unauthorized` | Invalid/expired JWT | Login again to get new token |
| `400 Bad Request: Payment amount must match` | Payment ≠ SIP amount | Create payment with exact SIP amount |
| `400 Bad Request: Payment must be PENDING` | Payment already used | Create new payment |
| `404 Not Found` | Resource doesn't exist | Verify IDs in error message |
| `405 Method Not Allowed` | Wrong HTTP method | Use POST/GET as specified in API docs |

## Branch Strategy

- `main` - Production ready code
- `feature/user-auth-module` - User authentication & management
- `feature/payment-module` - Payment processing (integrated)
- Other feature branches - Upcoming modules

Never push directly to `main`. Open a PR when work is complete.

## Security Notes

- ✅ JWT tokens expire in 24 hours
- ✅ Passwords hashed with BCrypt
- ✅ CORS configured for specific origins
- ✅ All endpoints except signup/signin require authentication
- ✅ Payment validation prevents unauthorized transactions
- ✅ Transaction records immutable for audit trail
- ⚠️ Never hardcode secrets in code
- ⚠️ Use `JWT_SECRET` >= 32 characters in production

## Contact & Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation files
- Review test guides for examples
