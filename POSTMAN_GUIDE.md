# WealthWise API — Complete Postman Testing Guide

> **Base URL (Local):** `http://localhost:9095`
> **Base URL (Production):** `https://wealthwise-backend.onrender.com`
> **Content-Type:** `application/json` on all requests
> **Auth:** `Authorization: Bearer {{token}}` on protected routes

---

## 📥 Step 1 — Download & Open Postman

1. Go to [postman.com/downloads](https://www.postman.com/downloads/)
2. Download and install **Postman** for macOS
3. Open Postman
4. Click **"Skip and go to the app"** if it asks you to sign in

---

## 📁 Step 2 — Create a Collection

1. In the **left sidebar** click **"Collections"**
2. Click the **"+"** button
3. Name it **`WealthWise API`**
4. Press **Enter** to save

---

## 🔑 Step 3 — Set Up Auto-Token Variable

This saves your JWT token automatically after signup/signin so you never have to copy-paste it manually.

1. Click on **`WealthWise API`** collection name in the sidebar
2. Click the **"Variables"** tab at the top
3. Add a new row:

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `token` | _(leave blank)_ | _(leave blank)_ |

4. Press **Cmd + S** to save

> ✅ After signup or signin, `{{token}}` will be auto-filled and used in all protected requests.

---

## ➕ Step 4 — How to Add a Request (Do this for every request below)

1. Click **"..."** next to your **`WealthWise API`** collection
2. Click **"Add request"**
3. Set the **name**, **method** (GET/POST/PATCH/DELETE) and **URL**
4. Go to **Headers tab** → add `Content-Type: application/json` (for requests with a body)
5. Go to **Body tab** → select **"raw"** → select **"JSON"** from the dropdown → paste the JSON
6. For protected routes → go to **Auth tab** → select **"Bearer Token"** → type `{{token}}`
7. Press **Cmd + S** to save

---

## 🔓 PUBLIC ENDPOINTS
### *(No token required)*

---

## ① POST `/auth/signup`

**What it does:** Creates a new user and returns a JWT token.
**Saves to DB:** ✅ Inserts into `users` table

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/signup` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
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

**Tests tab — paste this to auto-save token:**
```javascript
var jsonData = pm.response.json();
if (jsonData.token) {
    pm.collectionVariables.set("token", jsonData.token);
    console.log("✅ Token saved:", jsonData.token);
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

---

## ② POST `/auth/signin`

**What it does:** Logs in and returns a JWT token.
**Saves to DB:** ❌ Read only

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/signin` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
```json
{
  "email": "krish@wealthwise.com",
  "password": "Secret@123"
}
```

**Tests tab — paste this to auto-save token:**
```javascript
var jsonData = pm.response.json();
if (jsonData.token) {
    pm.collectionVariables.set("token", jsonData.token);
    console.log("✅ Token saved:", jsonData.token);
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

---

## ③ POST `/auth/forgot-password`

**What it does:** Generates a password reset token (valid 15 minutes).
**Saves to DB:** ✅ Inserts into `password_reset_tokens` table

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/forgot-password` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
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

> 📌 The reset token is saved in the DB. To get it, run this in your **Neon console**:
> ```sql
> SELECT token FROM password_reset_tokens ORDER BY created_at DESC LIMIT 1;
> ```

---

## ④ POST `/auth/reset-password`

**What it does:** Resets the password using the token from the DB.
**Saves to DB:** ✅ Updates `password_hash` in `users`, marks token as `used = true`

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/reset-password` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
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

## 🔒 PROTECTED ENDPOINTS
### *(Token required — add to every request below)*

> ### How to add the token in Postman:
> 1. Click the **"Auth"** tab on the request
> 2. Select **"Bearer Token"** from the dropdown
> 3. In the Token field type: `{{token}}`
> 4. That's it — Postman will automatically use the saved token ✅

---

## ⑤ POST `/auth/signout`

**What it does:** Signs out (stateless — client discards the token).
**Saves to DB:** ❌ No

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/signout` |

**Auth tab:**
| Type | Value |
|------|-------|
| Bearer Token | `{{token}}` |

**Body:** None (leave empty)

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

## ⑥ GET `/auth/me`

**What it does:** Returns the profile of the currently logged-in user.
**Saves to DB:** ❌ Read only

### In Postman:

| Field | Value |
|-------|-------|
| Method | `GET` |
| URL | `http://localhost:9095/auth/me` |

**Auth tab:**
| Type | Value |
|------|-------|
| Bearer Token | `{{token}}` |

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

## ⑦ PATCH `/auth/profile`

**What it does:** Updates first name, last name, and phone.
**Saves to DB:** ✅ Updates `first_name`, `last_name`, `phone`, `updated_at` in `users`

### In Postman:

| Field | Value |
|-------|-------|
| Method | `PATCH` |
| URL | `http://localhost:9095/auth/profile` |

**Auth tab:**
| Type | Value |
|------|-------|
| Bearer Token | `{{token}}` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
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

## ⑧ POST `/auth/change-password`

**What it does:** Changes the password after verifying the current one.
**Saves to DB:** ✅ Updates `password_hash` in `users`

### In Postman:

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `http://localhost:9095/auth/change-password` |

**Auth tab:**
| Type | Value |
|------|-------|
| Bearer Token | `{{token}}` |

**Headers tab:**
| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |

**Body tab → raw → JSON:**
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

> ⚠️ After changing password, sign in again with `Updated@789` to get a fresh token.

---

## ⑨ DELETE `/auth/account`

**What it does:** Permanently deletes the user and all their reset tokens.
**Saves to DB:** ✅ Deletes from `password_reset_tokens` then `users`

> ⚠️ **This is irreversible.**

### In Postman:

| Field | Value |
|-------|-------|
| Method | `DELETE` |
| URL | `http://localhost:9095/auth/account` |

**Auth tab:**
| Type | Value |
|------|-------|
| Bearer Token | `{{token}}` |

**Body:** None

**Expected Response `200 OK`:**
```json
{
  "success": true
}
```

---

## ❌ Common Error Responses & Fixes

| Error | HTTP Code | Message | Fix |
|-------|-----------|---------|-----|
| No token / wrong token | `401` | *(empty)* | Add `Bearer {{token}}` in Auth tab |
| Duplicate email | `409` | `Email already in use` | Use a different email |
| Wrong password | `401` | `Invalid email or password` | Check your password |
| Expired reset token | `400` | `Reset token has expired` | Call forgot-password again |
| Already used reset token | `400` | `Reset token has already been used` | Call forgot-password again |
| Missing field in body | `400` | `Bad Request` | Check all required fields are filled |
| User not found | `404` | `User not found` | Check the user exists in DB |

---

## ✅ Recommended Test Order

Follow this exact order for a complete end-to-end test:

| # | Method | Endpoint | Token | DB Write |
|---|--------|----------|-------|----------|
| 1 | POST | `/auth/signup` | ❌ | ✅ `users` insert |
| 2 | POST | `/auth/signin` | ❌ | ❌ read only |
| 3 | GET | `/auth/me` | ✅ | ❌ read only |
| 4 | PATCH | `/auth/profile` | ✅ | ✅ `users` update |
| 5 | POST | `/auth/change-password` | ✅ | ✅ `users` update |
| 6 | POST | `/auth/forgot-password` | ❌ | ✅ token insert |
| 7 | POST | `/auth/reset-password` | ❌ | ✅ token + user update |
| 8 | POST | `/auth/signout` | ✅ | ❌ stateless |
| 9 | DELETE | `/auth/account` | ✅ | ✅ both tables delete |

---

## 🌐 Testing on Production (Render)

Once deployed, replace `http://localhost:9095` with your Render URL in every request:

| Local | Production |
|-------|------------|
| `http://localhost:9095/auth/signup` | `https://wealthwise-backend.onrender.com/auth/signup` |
| `http://localhost:9095/auth/signin` | `https://wealthwise-backend.onrender.com/auth/signin` |
| `http://localhost:9095/auth/me` | `https://wealthwise-backend.onrender.com/auth/me` |
| `http://localhost:9095/auth/profile` | `https://wealthwise-backend.onrender.com/auth/profile` |
| `http://localhost:9095/auth/change-password` | `https://wealthwise-backend.onrender.com/auth/change-password` |
| `http://localhost:9095/auth/forgot-password` | `https://wealthwise-backend.onrender.com/auth/forgot-password` |
| `http://localhost:9095/auth/reset-password` | `https://wealthwise-backend.onrender.com/auth/reset-password` |
| `http://localhost:9095/auth/signout` | `https://wealthwise-backend.onrender.com/auth/signout` |
| `http://localhost:9095/auth/account` | `https://wealthwise-backend.onrender.com/auth/account` |

> 💡 Tip: Create two Postman environments — **Local** and **Production** — and set `baseUrl` as a variable so you can switch with one click.

---

## 🗄️ Verify in Neon DB

After running requests, verify in your [Neon console](https://console.neon.tech):

```sql
-- See all users
SELECT id, first_name, last_name, email, phone, role, kyc_verified, enabled, created_at, updated_at
FROM users;

-- See password reset tokens
SELECT t.token, t.expires_at, t.used, u.email
FROM password_reset_tokens t
JOIN users u ON u.id = t.user_id
ORDER BY t.created_at DESC;
```

---

## 💡 Pro Tips

| Tip | Detail |
|-----|--------|
| **Auto token** | Paste the Tests script in both signup AND signin so `{{token}}` always stays fresh |
| **Duplicate test** | Run signup twice with same email → should get `409 Conflict` |
| **Wrong password test** | Use wrong password on signin → should get `401 Unauthorized` |
| **No token test** | Call `/auth/me` without token → should get `401 Unauthorized` |
| **Token expiry** | Token expires in 24h — just signin again to get a new one |
| **Render wake up** | First request on Render free tier takes ~30 sec — just wait and retry |

