# Postman Testing Guide - Funds Module (WealthWise)

This guide is a complete step-by-step checklist to test all Funds Module APIs in Postman.

## 1) Prerequisites

- Backend is running locally.
- You already have auth APIs working (`/auth/signup`, `/auth/signin`).
- PostgreSQL (Neon) connection is configured.

Run backend from project root:

```bash
./mvnw spring-boot:run
```

Default local URL used in this guide:

- `http://localhost:9095`

## 2) Create Postman Collection and Environment

### 2.1 Create collection

- Create collection: `WealthWise - Funds Module`.

### 2.2 Create environment variables

Create a Postman environment with these variables:

- `base_url` = `http://localhost:9095`
- `email` = `your_test_email@example.com`
- `password` = `YourStrongPassword@123`
- `jwt_token` = (leave empty initially)
- `fund_id` = `122639`

## 3) Get JWT Token (Required for protected APIs)

Funds `invest` and all `bookmarks` APIs need JWT.

### 3.1 Sign up (only once per email)

- Method: `POST`
- URL: `{{base_url}}/auth/signup`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "{{email}}",
  "phone": "9876543210",
  "countryCode": "+91",
  "password": "{{password}}"
}
```

Expected:

- Status `201`
- Response contains `token`

### 3.2 Sign in

- Method: `POST`
- URL: `{{base_url}}/auth/signin`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Expected:

- Status `200`
- Response contains `token`

### 3.3 Auto-save token in Postman

Add this in the request `Tests` tab (signup and signin):

```javascript
const json = pm.response.json();
if (json.token) {
  pm.environment.set("jwt_token", json.token);
}
```

For protected requests, set header:

- `Authorization: Bearer {{jwt_token}}`

## 4) Funds Module APIs - Step by Step

## 4.1 Search funds

- Method: `GET`
- URL examples:
  - `{{base_url}}/funds`
  - `{{base_url}}/funds?search=hdfc`
  - `{{base_url}}/funds?category=Equity`
  - `{{base_url}}/funds?search=axis&category=Debt`
- Auth: Not required

Expected:

- Status `200`
- Response is an array of funds

## 4.2 Get fund details by id

- Method: `POST`
- URL: `{{base_url}}/funds`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "id": "{{fund_id}}"
}
```

Expected:

- Status `200`
- Response contains complete fund object

Negative test:

- Body with invalid id:

```json
{
  "id": "999999999"
}
```

Expected:

- Usually `404` if not found from DB/API

## 4.3 Get NAV history

- Method: `GET`
- URL examples:
  - `{{base_url}}/funds/{{fund_id}}/nav-history`
  - `{{base_url}}/funds/{{fund_id}}/nav-history?period=1M`
  - `{{base_url}}/funds/{{fund_id}}/nav-history?period=3M`
  - `{{base_url}}/funds/{{fund_id}}/nav-history?period=6M`
  - `{{base_url}}/funds/{{fund_id}}/nav-history?period=1Y`
  - `{{base_url}}/funds/{{fund_id}}/nav-history?period=ALL`
- Auth: Not required

Expected:

- Status `200`
- Response format:

```json
{
  "data": [
    {
      "date": "2026-01-01",
      "nav": 123.45
    }
  ]
}
```

Negative test:

- Use invalid `fund_id`
- Expected `404`

## 4.4 Invest in fund (protected)

- Method: `POST`
- URL: `{{base_url}}/funds/{{fund_id}}/invest`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{jwt_token}}`

Valid SIP body:

```json
{
  "type": "SIP",
  "amount": 5000
}
```

Valid Lumpsum body:

```json
{
  "type": "Lumpsum",
  "amount": 50000
}
```

Expected:

- Status `200`
- Response:

```json
{
  "success": true
}
```

Important note:

- Current implementation validates input and returns success.
- Actual transaction/holding creation is still placeholder logic.

Negative tests:

1) Invalid amount

```json
{
  "type": "SIP",
  "amount": 0
}
```

Expected: `400`

2) Invalid type

```json
{
  "type": "Monthly",
  "amount": 5000
}
```

Expected: `400`

3) Missing token

- Remove `Authorization` header
- Expected: `401`

## 4.5 Get bookmarks (protected)

- Method: `GET`
- URL: `{{base_url}}/bookmarks`
- Header: `Authorization: Bearer {{jwt_token}}`

Expected:

- Status `200`
- Response:

```json
{
  "fundIds": ["122639", "120503"]
}
```

## 4.6 Add bookmark (protected)

- Method: `POST`
- URL: `{{base_url}}/bookmarks/{{fund_id}}`
- Header: `Authorization: Bearer {{jwt_token}}`

Expected:

- Status `200`
- Response:

```json
{
  "success": true
}
```

Negative test:

- Invalid `fund_id` should return `404`

## 4.7 Remove bookmark (protected)

- Method: `DELETE`
- URL: `{{base_url}}/bookmarks/{{fund_id}}`
- Header: `Authorization: Bearer {{jwt_token}}`

Expected:

- Status `200`
- Response:

```json
{
  "success": true
}
```

## 5) Recommended Full Test Order

Run in this exact order:

1. `POST /auth/signup`
2. `POST /auth/signin`
3. `GET /funds`
4. `GET /funds?search=hdfc`
5. `POST /funds` (fund details)
6. `GET /funds/{id}/nav-history` (all periods)
7. `POST /funds/{id}/invest` (valid + invalid)
8. `GET /bookmarks`
9. `POST /bookmarks/{fundId}`
10. `GET /bookmarks`
11. `DELETE /bookmarks/{fundId}`
12. `GET /bookmarks`

## 6) Database Verification (Neon SQL)

Run these queries in Neon SQL editor after testing.

Check seeded/search-saved funds:

```sql
SELECT id, name, category, nav, created_at, updated_at
FROM funds
ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST
LIMIT 30;
```

Check NAV history rows:

```sql
SELECT fund_id, date, nav
FROM fund_nav_history
WHERE fund_id = '122639'
ORDER BY date DESC
LIMIT 30;
```

Check user bookmarks:

```sql
SELECT b.id, b.user_id, b.fund_id, b.created_at
FROM bookmarks b
ORDER BY b.created_at DESC
LIMIT 30;
```

Check if bookmark removed:

```sql
SELECT *
FROM bookmarks
WHERE user_id = '<your_user_uuid>'
  AND fund_id = '122639';
```

Expected after delete: `0 rows`.

## 7) Common Issues and Fixes

### 7.1 401 Unauthorized

- Token missing/expired/invalid.
- Fix: sign in again and ensure `Authorization: Bearer {{jwt_token}}`.

### 7.2 404 Fund not found

- Invalid scheme code.
- Fix: use known ids like `122639`, `120503`, `125354`.

### 7.3 400 Bad Request on invest

- Invalid `type` or `amount` below rules.
- Fix body according to validation.

### 7.4 Empty search result

- Search text/category does not match available funds.
- Fix: try broader query and first test `GET /funds` without filters.

### 7.5 CORS issue in frontend but Postman works

- Postman bypasses browser CORS policy.
- Verify backend CORS allowed origins and exact frontend URL.

## 8) Production Testing

When deployed, change `base_url` only.

Example:

- `base_url = https://your-render-service.onrender.com`

Then all requests keep working with same collection.

---

If you want, next I can also generate a ready-to-import Postman collection JSON file for these funds endpoints.
