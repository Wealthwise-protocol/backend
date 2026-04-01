# WealthWise User Module - Postman Testing Guide

Complete guide to test all 9 User Authentication APIs using Postman.

---

## 📋 Table of Contents

1. [Setup](#setup)
2. [Environment Variables](#environment-variables)
3. [API Tests](#api-tests)
   - [1. Sign Up](#1-sign-up)
   - [2. Sign Up - Duplicate Email](#2-sign-up---duplicate-email)
   - [3. Sign In](#3-sign-in)
   - [4. Sign In - Invalid Password](#4-sign-in---invalid-password)
   - [5. Get Current User](#5-get-current-user)
   - [6. Get Current User - No Token](#6-get-current-user---no-token)
   - [7. Update Profile](#7-update-profile)
   - [8. Change Password](#8-change-password)
   - [9. Change Password - Wrong Current](#9-change-password---wrong-current)
   - [10. Forgot Password](#10-forgot-password)
   - [11. Reset Password](#11-reset-password)
   - [12. Reset Password - Invalid OTP](#12-reset-password---invalid-otp)
   - [13. Sign Out](#13-sign-out)
   - [14. Delete Account](#14-delete-account)
   - [15. Sign In After Deletion](#15-sign-in-after-deletion)

---

## Setup

### Base URL
```
Production: https://wealthwise-backend.onrender.com
Local: http://localhost:9095
```

### Create New Collection
1. Open Postman
2. Click "New" → "Collection"
3. Name: `WealthWise - User Module`
4. Save

---

## Environment Variables

### Create Environment
1. Click "Environments" (left sidebar)
2. Click "+" to create new environment
3. Name: `WealthWise Production`

### Add Variables

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `base_url` | `https://wealthwise-backend.onrender.com` | `https://wealthwise-backend.onrender.com` |
| `token` | (leave empty) | (auto-populated) |
| `test_email` | `test_user_{{$timestamp}}@example.com` | (auto-populated) |
| `test_password` | `TestPass123!` | `TestPass123!` |
| `user_id` | (leave empty) | (auto-populated) |

4. Click "Save"
5. Select this environment from dropdown (top-right)

---

## API Tests

---

## 1. Sign Up

**Create New User Account**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signup`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "{{test_email}}",
  "phone": "9876543210",
  "countryCode": "+91",
  "password": "{{test_password}}"
}
```

### Expected Response
**Status**: `201 Created`

```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Test",
    "lastName": "User",
    "email": "test_user_1234567890@example.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Tests Script (Postman Tests Tab)
```javascript
// Save token and user_id for subsequent requests
if (pm.response.code === 201) {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
    pm.environment.set("user_id", jsonData.user.id);
    pm.test("Status code is 201", () => {
        pm.response.to.have.status(201);
    });
    pm.test("Token is present", () => {
        pm.expect(jsonData.token).to.exist;
    });
    pm.test("User ID is present", () => {
        pm.expect(jsonData.user.id).to.exist;
    });
}
```

---

## 2. Sign Up - Duplicate Email

**Test duplicate email validation**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signup`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "{{test_email}}",
  "phone": "9876543211",
  "countryCode": "+91",
  "password": "{{test_password}}"
}
```

### Expected Response
**Status**: `409 Conflict`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email already in use",
  "path": "/auth/signup"
}
```

### Tests Script
```javascript
pm.test("Status code is 409", () => {
    pm.response.to.have.status(409);
});
pm.test("Error message is correct", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Email already in use");
});
```

---

## 3. Sign In

**Login with existing credentials**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signin`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "email": "{{test_email}}",
  "password": "{{test_password}}"
}
```

### Expected Response
**Status**: `200 OK`

```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Test",
    "lastName": "User",
    "email": "test_user_1234567890@example.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Tests Script
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
    pm.test("Status code is 200", () => {
        pm.response.to.have.status(200);
    });
    pm.test("Token is present", () => {
        pm.expect(jsonData.token).to.exist;
    });
}
```

---

## 4. Sign In - Invalid Password

**Test invalid credentials**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signin`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "email": "{{test_email}}",
  "password": "WrongPassword123!"
}
```

### Expected Response
**Status**: `401 Unauthorized`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/auth/signin"
}
```

### Tests Script
```javascript
pm.test("Status code is 401", () => {
    pm.response.to.have.status(401);
});
pm.test("Error message is correct", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Invalid email or password");
});
```

---

## 5. Get Current User

**Fetch authenticated user details**

### Request Details
- **Method**: `GET`
- **URL**: `{{base_url}}/auth/me`
- **Headers**: 
  ```
  Authorization: Bearer {{token}}
  ```

### Body
(No body required)

### Expected Response
**Status**: `200 OK`

```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Test",
    "lastName": "User",
    "email": "test_user_1234567890@example.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  }
}
```

### Tests Script
```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});
pm.test("User object is present", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.user).to.exist;
    pm.expect(jsonData.user.email).to.exist;
});
```

---

## 6. Get Current User - No Token

**Test unauthorized access**

### Request Details
- **Method**: `GET`
- **URL**: `{{base_url}}/auth/me`
- **Headers**: 
  ```
  (No Authorization header)
  ```

### Body
(No body required)

### Expected Response
**Status**: `401 Unauthorized`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/auth/me"
}
```

### Tests Script
```javascript
pm.test("Status code is 401", () => {
    pm.response.to.have.status(401);
});
```

---

## 7. Update Profile

**Update user profile information**

### Request Details
- **Method**: `PATCH`
- **URL**: `{{base_url}}/auth/profile`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{token}}
  ```

### Body (JSON)
```json
{
  "firstName": "Updated",
  "lastName": "Name",
  "phone": "9876543210"
}
```

### Expected Response
**Status**: `200 OK`

```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Updated",
    "lastName": "Name",
    "email": "test_user_1234567890@example.com",
    "phone": "9876543210",
    "countryCode": "+91",
    "kycVerified": false
  }
}
```

### Tests Script
```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});
pm.test("Profile updated", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.user.firstName).to.eql("Updated");
    pm.expect(jsonData.user.lastName).to.eql("Name");
});
```

---

## 8. Change Password

**Change user password**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/change-password`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{token}}
  ```

### Body (JSON)
```json
{
  "currentPassword": "{{test_password}}",
  "newPassword": "NewPass456!"
}
```

### Expected Response
**Status**: `200 OK`

```json
{
  "success": true
}
```

### Tests Script
```javascript
if (pm.response.code === 200) {
    pm.environment.set("test_password", "NewPass456!");
    pm.test("Status code is 200", () => {
        pm.response.to.have.status(200);
    });
    pm.test("Success is true", () => {
        var jsonData = pm.response.json();
        pm.expect(jsonData.success).to.be.true;
    });
}
```

---

## 9. Change Password - Wrong Current

**Test wrong current password validation**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/change-password`
- **Headers**: 
  ```
  Content-Type: application/json
  Authorization: Bearer {{token}}
  ```

### Body (JSON)
```json
{
  "currentPassword": "WrongPassword123!",
  "newPassword": "AnotherPass789!"
}
```

### Expected Response
**Status**: `401 Unauthorized`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Current password is incorrect",
  "path": "/auth/change-password"
}
```

### Tests Script
```javascript
pm.test("Status code is 401", () => {
    pm.response.to.have.status(401);
});
pm.test("Error message is correct", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Current password is incorrect");
});
```

---

## 10. Forgot Password

**Request password reset OTP**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/forgot-password`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "email": "{{test_email}}"
}
```

### Expected Response
**Status**: `200 OK`

```json
{
  "message": "Reset link sent"
}
```

### Tests Script
```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});
pm.test("Message is correct", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Reset link sent");
});
```

### ⚠️ Manual Step Required
**Check your email for 6-digit OTP** (e.g., `847293`)

Then manually set the OTP in environment:
1. Go to Environments
2. Add variable: `reset_otp` = `<your_otp_from_email>`
3. Save

---

## 11. Reset Password

**Reset password using OTP**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/reset-password`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "otp": "{{reset_otp}}",
  "newPassword": "ResetPass999!"
}
```

### Expected Response
**Status**: `200 OK`

```json
{
  "message": "Password reset successfully"
}
```

### Tests Script
```javascript
if (pm.response.code === 200) {
    pm.environment.set("test_password", "ResetPass999!");
    pm.test("Status code is 200", () => {
        pm.response.to.have.status(200);
    });
    pm.test("Message is correct", () => {
        var jsonData = pm.response.json();
        pm.expect(jsonData.message).to.include("Password reset successfully");
    });
}
```

---

## 12. Reset Password - Invalid OTP

**Test invalid OTP validation**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/reset-password`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "otp": "999999",
  "newPassword": "TestPass123!"
}
```

### Expected Response
**Status**: `400 Bad Request`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid OTP",
  "path": "/auth/reset-password"
}
```

### Tests Script
```javascript
pm.test("Status code is 400", () => {
    pm.response.to.have.status(400);
});
pm.test("Error message is correct", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Invalid OTP");
});
```

---

## 13. Sign Out

**Logout user**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signout`
- **Headers**: 
  ```
  Authorization: Bearer {{token}}
  ```

### Body
(No body required)

### Expected Response
**Status**: `200 OK`

```json
{
  "success": true
}
```

### Tests Script
```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});
pm.test("Success is true", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
});
```

---

## 14. Delete Account

**Permanently delete user account**

### Request Details
- **Method**: `DELETE`
- **URL**: `{{base_url}}/auth/account`
- **Headers**: 
  ```
  Authorization: Bearer {{token}}
  ```

### Body
(No body required)

### Expected Response
**Status**: `200 OK`

```json
{
  "success": true
}
```

### Tests Script
```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});
pm.test("Success is true", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
});
```

---

## 15. Sign In After Deletion

**Verify account is deleted**

### Request Details
- **Method**: `POST`
- **URL**: `{{base_url}}/auth/signin`
- **Headers**: 
  ```
  Content-Type: application/json
  ```

### Body (JSON)
```json
{
  "email": "{{test_email}}",
  "password": "{{test_password}}"
}
```

### Expected Response
**Status**: `401 Unauthorized`

```json
{
  "timestamp": "2024-03-25T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/auth/signin"
}
```

### Tests Script
```javascript
pm.test("Status code is 401", () => {
    pm.response.to.have.status(401);
});
pm.test("Account is deleted", () => {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Invalid email or password");
});
```

---

## 🎯 Testing Order

Run tests in this exact order for best results:

1. ✅ Sign Up
2. ✅ Sign Up - Duplicate Email
3. ✅ Sign In
4. ✅ Sign In - Invalid Password
5. ✅ Get Current User
6. ✅ Get Current User - No Token
7. ✅ Update Profile
8. ✅ Change Password
9. ✅ Change Password - Wrong Current
10. ✅ Forgot Password
11. ⚠️ **Manual**: Check email for OTP
12. ✅ Reset Password
13. ✅ Reset Password - Invalid OTP
14. ✅ Sign Out
15. ✅ Delete Account
16. ✅ Sign In After Deletion

---

## 📊 Expected Results Summary

| Test | Expected Status | Pass Criteria |
|------|----------------|---------------|
| Sign Up | 201 | Token + User returned |
| Duplicate Email | 409 | Error message |
| Sign In | 200 | Token + User returned |
| Invalid Password | 401 | Error message |
| Get Current User | 200 | User object returned |
| No Token | 401 | Unauthorized |
| Update Profile | 200 | Updated user returned |
| Change Password | 200 | Success true |
| Wrong Current Password | 401 | Error message |
| Forgot Password | 200 | Message sent |
| Reset Password | 200 | Success message |
| Invalid OTP | 400 | Error message |
| Sign Out | 200 | Success true |
| Delete Account | 200 | Success true |
| Sign In After Deletion | 401 | Unauthorized |

---

## 🔧 Troubleshooting

### Issue: Token not auto-saving
**Solution**: Make sure you've added the Tests script to Sign Up and Sign In requests

### Issue: Email not received
**Solutions**:
1. Check spam/junk folder
2. Verify Gmail SMTP is configured on Render
3. Check Render logs for email errors
4. Try with a different email provider

### Issue: 401 Unauthorized on protected routes
**Solutions**:
1. Verify token is saved in environment variables
2. Check token hasn't expired (24-hour limit)
3. Re-run Sign In to get fresh token

### Issue: OTP expired
**Solution**: OTP expires in 15 minutes. Request a new one via Forgot Password

---

## 🚀 Quick Start

1. **Import Collection**: Create new collection in Postman
2. **Setup Environment**: Add all variables listed above
3. **Run Sign Up**: This will auto-save token
4. **Run Tests in Order**: Follow the testing order above
5. **Check Email**: For OTP in Forgot Password flow

---

## 📝 Notes

- **Token Expiry**: JWT tokens expire after 24 hours
- **OTP Expiry**: Password reset OTPs expire after 15 minutes
- **OTP Format**: Must be exactly 6 digits
- **Email**: Case-insensitive (automatically normalized)
- **Cascade Delete**: Deleting account removes all related data (SIPs, Bookmarks, Tokens)

---

**Happy Testing! 🎉**
