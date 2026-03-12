# WealthWise API — Postman Testing Guide

> **Base URL:** `http://localhost:9095` (local) or your Railway/Render URL (deployed)  
> **Server Port:** `9095` (local) / `8080` (deployed)  
> **Database:** Neon PostgreSQL (`wealthwise_db`)  
> **Content-Type:** `application/json` on all requests

---

## ⚙️ Pre-requisites

1. Start the server:
   ```bash
   ./mvnw spring-boot:run
   ```
2. In Postman, create a **Collection Variable** called `token` (leave blank for now — it gets filled automatically in Step 2).
3. Set `Content-Type: application/json` header on every request.

---

## 🔐 Authentication Endpoints (No token required)

---

### 1. POST `/auth/signup`

**Description:** Creates a new user and returns a JWT token.  
**Saves to DB:** ✅ Inserts a row into `users` table.

**URL:**
```
POST http://localhost:8083/auth/signup
```

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "firstName": "Krish",
  "lastName": "Malvia",
  "email": "krish@wealthwise.com",
  "phone": "9876543210",
  "countryCode": "+91",
  "password": "Secret@123"
}
```

**Expected Response `201 Created`:**
```json
{
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "Krish",
    "lastName": "Malvia",
    "email": "krish@wealthwise.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Postman Test Script (Auto-save token):**  
Paste this in the **Tests** tab of this request:
```javascript
var jsonData = pm.response.json();
pm.collectionVariables.set("token", jsonData.token);
console.log("Token saved:", jsonData.token);
```

---

### 2. POST `/auth/signin`

**Description:** Sign in with email and password, returns a JWT token.  
**Saves to DB:** ❌ Read-only (no DB write).

**URL:**
```
POST http://localhost:8083/auth/signin
```

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "email": "krish@wealthwise.com",
  "password": "Secret@123"
}
```

**Expected Response `200 OK`:**
```json
{
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "Krish",
    "lastName": "Malvia",
    "email": "krish@wealthwise.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Postman Test Script (Auto-save token):**
```javascript
var jsonData = pm.response.json();
pm.collectionVariables.set("token", jsonData.token);
console.log("Token saved:", jsonData.token);
```

---

### 3. POST `/auth/forgot-password`

**Description:** Generates a password reset token (valid 15 min).  
**Saves to DB:** ✅ Inserts a row into `password_reset_tokens` table.

**URL:**
```
POST http://localhost:8083/auth/forgot-password
```

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "email": "krish@wealthwise.com"
}
```

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

> ⚠️ The reset token is stored in the DB (`password_reset_tokens` table).  
> Run this SQL to get it for testing:
> ```sql
> SELECT token, expires_at FROM password_reset_tokens ORDER BY created_at DESC LIMIT 1;
> ```

---

### 4. POST `/auth/reset-password`

**Description:** Resets password using the token from the DB.  
**Saves to DB:** ✅ Updates `password_hash` in `users` table. Marks token as `used = true`.

**URL:**
```
POST http://localhost:8083/auth/reset-password
```

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "token": "PASTE_TOKEN_FROM_DB_HERE",
  "newPassword": "NewSecret@456"
}
```

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

## 🔒 Protected Endpoints (JWT Token required)

> Add this header to ALL requests below:
> ```
> Authorization: Bearer {{token}}
> ```
> Or if pasting manually:
> ```
> Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
> ```

---

### 5. POST `/auth/signout`

**Description:** Stateless signout — client discards the token.  
**Saves to DB:** ❌ No DB write (stateless JWT).

**URL:**
```
POST http://localhost:8083/auth/signout
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body:** None (empty)

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

### 6. GET `/auth/me`

**Description:** Returns the currently authenticated user's profile.  
**Saves to DB:** ❌ Read-only.

**URL:**
```
GET http://localhost:8083/auth/me
```

**Headers:**
```
Authorization: Bearer {{token}}
```

**Body:** None

**Expected Response `200 OK`:**
```json
{
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "Krish",
    "lastName": "Malvia",
    "email": "krish@wealthwise.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  }
}
```

---

### 7. PATCH `/auth/profile`

**Description:** Updates first name, last name, and phone.  
**Saves to DB:** ✅ Updates `users` table (`first_name`, `last_name`, `phone`, `updated_at`).

**URL:**
```
PATCH http://localhost:8083/auth/profile
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (raw JSON):**
```json
{
  "firstName": "Krishna",
  "lastName": "Malviya",
  "phone": "9123456789"
}
```

**Expected Response `200 OK`:**
```json
{
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "Krishna",
    "lastName": "Malviya",
    "email": "krish@wealthwise.com",
    "phone": "9123456789",
    "countryCode": "+91",
    "kycVerified": false
  }
}
```

---

### 8. POST `/auth/change-password`

**Description:** Changes password (requires current password).  
**Saves to DB:** ✅ Updates `password_hash` in `users` table.

**URL:**
```
POST http://localhost:8083/auth/change-password
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (raw JSON):**
```json
{
  "currentPassword": "Secret@123",
  "newPassword": "Updated@789"
}
```

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

### 9. DELETE `/auth/account`

**Description:** Permanently deletes the user and all associated password reset tokens.  
**Saves to DB:** ✅ Deletes from `password_reset_tokens` then `users` table.

> ⚠️ **Irreversible.** The user will be permanently removed from the database.

**URL:**
```
DELETE http://localhost:8083/auth/account
```

**Headers:**
```
Authorization: Bearer {{token}}
```

**Body:** None

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

## ❌ Error Response Examples

### Duplicate email on signup
```json
HTTP 409 Conflict
{
  "status": 409,
  "error": "Conflict",
  "message": "Email already in use"
}
```

### Wrong password on signin
```json
HTTP 401 Unauthorized
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

### Missing / expired token on protected route
```json
HTTP 401 Unauthorized
```

### Expired reset token
```json
HTTP 400 Bad Request
{
  "status": 400,
  "error": "Bad Request",
  "message": "Reset token has expired"
}
```

---

## ✅ Recommended Test Order in Postman

| Step | Method | Endpoint | DB Write? |
|------|--------|----------|-----------|
| 1 | POST | `/auth/signup` | ✅ `users` insert |
| 2 | POST | `/auth/signin` | ❌ read only |
| 3 | GET | `/auth/me` | ❌ read only |
| 4 | PATCH | `/auth/profile` | ✅ `users` update |
| 5 | POST | `/auth/change-password` | ✅ `users` update |
| 6 | POST | `/auth/forgot-password` | ✅ `password_reset_tokens` insert |
| 7 | POST | `/auth/reset-password` | ✅ `users` + token update |
| 8 | POST | `/auth/signout` | ❌ stateless |
| 9 | DELETE | `/auth/account` | ✅ both tables delete |

---

## 🗄️ DB Verification SQL (Neon Console or psql)

After running each request, verify in your Neon DB:

```sql
-- Check all users
SELECT id, first_name, last_name, email, phone, role, kyc_verified, enabled, created_at
FROM users;

-- Check password reset tokens
SELECT t.token, t.expires_at, t.used, u.email
FROM password_reset_tokens t
JOIN users u ON u.id = t.user_id
ORDER BY t.created_at DESC;
```

---

## 💡 Tips

- Copy the `token` from `/signup` or `/signin` response and paste it into the `Authorization: Bearer <token>` header for all protected routes.
- If you use Postman **Collection Variables**, the **Test Script** snippets above will auto-fill `{{token}}` for you.
- Tokens expire in **24 hours** by default (`security.jwt.expiration-ms=86400000`).
- Reset tokens expire in **15 minutes**.

