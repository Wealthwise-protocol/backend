# WealthWise Transaction Module - Postman Testing Guide

Complete guide to test the Transaction APIs using Postman with detailed request bodies and responses.

---

## 📋 Table of Contents

1. [Setup](#setup)
2. [Environment Variables](#environment-variables)
3. [Prerequisites](#prerequisites)
4. [API Tests](#api-tests)
   - [1. Get All Transactions](#1-get-all-transactions)
   - [2. Get Transactions by Type](#2-get-transactions-by-type)
   - [3. Get Transactions Sorted Ascending](#3-get-transactions-sorted-ascending)
   - [4. Create Transaction (BUY)](#4-create-transaction-buy)
   - [5. Create Transaction (SELL)](#5-create-transaction-sell)
   - [6. Create Transaction (Dividend)](#6-create-transaction-dividend)
   - [7. Get All Transactions (After Creation)](#7-get-all-transactions-after-creation)
   - [8. Filter by Type (BUY)](#8-filter-by-type-buy)
   - [9. Error Cases](#9-error-cases)

---

## Setup

### Base URL
```
Development/Local: http://localhost:9095
Production: https://wealthwise-backend.onrender.com
```

### Create New Collection

1. Open Postman
2. Click "New" → "Collection"
3. Name: `WealthWise - Transaction Module`
4. Click "Create"

---

## Environment Variables

### Create Environment

1. In Postman, click "Environments" (left sidebar)
2. Click "+" to create new environment
3. Name: `WealthWise - Local`
4. Add the following variables:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| baseUrl | http://localhost:9095 | http://localhost:9095 |
| token | (empty) | (will be set after login) |
| userId | (empty) | (will be set after login) |
| fundId | (empty) | (will be set after fetching funds) |

5. Click "Save"

---

## Prerequisites

### Step 1: Create a New User (Sign Up)

First, you need to create a user account.

**Endpoint:** POST `/users/auth/sign-up`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "SecurePassword@123"
}
```

**Headers:**
```
Content-Type: application/json
```

**Expected Response (201 Created):**
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "createdAt": "2024-03-15T10:30:00"
  }
}
```

> **Tip:** Save the `id` from the response as `{{userId}}` in your environment variables.

---

### Step 2: Sign In to Get Authentication Token

**Endpoint:** POST `/users/auth/sign-in`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword@123"
}
```

**Headers:**
```
Content-Type: application/json
```

**Expected Response (200 OK):**
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john.doe@example.com"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

> **Tip:** Save the `token` value. In Postman, you need to use this for all transaction requests.
> - Copy the token value
> - Go to Environment Variables
> - Set `token` to the value you copied
> - Or use: `{{token}}`

---

### Step 3: Get Available Funds

Before creating transactions, you need a valid Fund ID.

**Endpoint:** GET `/funds`

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Expected Response (200 OK):**
```json
{
  "funds": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "name": "Axis Bluechip Fund",
      "amc": "Axis Asset Management",
      "category": "Equity",
      "subcategory": "Large Cap",
      "risk": "High",
      "nav": 45.25,
      "minLumpsum": 500,
      "minSip": 1000
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440002",
      "name": "HDFC Balanced Advantage Fund",
      "amc": "HDFC Asset Management",
      "category": "Balanced",
      "risk": "Medium",
      "nav": 67.50,
      "minLumpsum": 500,
      "minSip": 500
    }
  ]
}
```

> **Tip:** Copy any Fund `id` and save it as `{{fundId}}` in your environment variables.

---

## API Tests

### 1. Get All Transactions

Retrieve all transactions for the authenticated user (sorted by date, descending by default).

**Method:** GET

**Endpoint:** 
```
{{baseUrl}}/transactions
```

**Full URL Example:**
```
http://localhost:9095/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Query Parameters:**
```
(none - optional parameters available)
```

**Request Body:**
```
(empty)
```

**cURL Example:**
```bash
curl --request GET \
  --url 'http://localhost:9095/transactions' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json'
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440003",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-15",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 50000.00,
      "units": 1104.97,
      "nav": 45.25,
      "status": "COMPLETED"
    }
  ]
}
```

**HTTP Status Codes:**
- `200 OK` - Successfully retrieved transactions
- `401 UNAUTHORIZED` - Missing or invalid token
- `404 NOT FOUND` - User not found

---

### 2. Get Transactions by Type

Filter transactions by type (BUY, SELL, or DIVIDEND).

**Method:** GET

**Endpoint:**
```
{{baseUrl}}/transactions?type=BUY
```

**Full URL Example:**
```
http://localhost:9095/transactions?type=BUY
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Query Parameters:**
```
type=BUY
```

Options for `type`:
- `BUY` - Buy transactions
- `SELL` - Sell transactions
- `DIVIDEND` - Dividend transactions

**Request Body:**
```
(empty)
```

**cURL Example:**
```bash
curl --request GET \
  --url 'http://localhost:9095/transactions?type=BUY' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json'
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440003",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-15",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 50000.00,
      "units": 1104.97,
      "nav": 45.25,
      "status": "COMPLETED"
    }
  ]
}
```

---

### 3. Get Transactions Sorted Ascending

Retrieve transactions sorted by date in ascending order (oldest first).

**Method:** GET

**Endpoint:**
```
{{baseUrl}}/transactions?sort=asc
```

**Full URL Example:**
```
http://localhost:9095/transactions?sort=asc
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Query Parameters:**
```
sort=asc
```

Options for `sort`:
- `asc` - Ascending order (oldest first)
- `desc` - Descending order (newest first) - **Default**

**Request Body:**
```
(empty)
```

**cURL Example:**
```bash
curl --request GET \
  --url 'http://localhost:9095/transactions?sort=asc' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json'
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440001",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-01-10",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 25000.00,
      "units": 552.49,
      "nav": 45.25,
      "status": "COMPLETED"
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-02-20",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 25000.00,
      "units": 552.49,
      "nav": 45.25,
      "status": "COMPLETED"
    }
  ]
}
```

---

### 4. Create Transaction (BUY)

Create a BUY transaction for a mutual fund.

**Method:** POST

**Endpoint:**
```
{{baseUrl}}/transactions
```

**Full URL Example:**
```
http://localhost:9095/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (RAW JSON):**
```json
{
  "fundId": "660e8400-e29b-41d4-a716-446655440001",
  "date": "2024-03-15",
  "fundName": "Axis Bluechip Fund",
  "type": "BUY",
  "amount": 50000.00,
  "units": 1104.97,
  "nav": 45.25,
  "status": "COMPLETED"
}
```

**Field Descriptions:**

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| fundId | UUID | Yes | ID of the fund (get from /funds endpoint) | "660e8400-e29b-41d4-a716-446655440001" |
| date | LocalDate (YYYY-MM-DD) | Yes | Transaction date | "2024-03-15" |
| fundName | String | Yes | Name of the fund | "Axis Bluechip Fund" |
| type | String | Yes | Transaction type (BUY, SELL, DIVIDEND) | "BUY" |
| amount | BigDecimal | Yes | Investment amount (> 0.01) | 50000.00 |
| units | BigDecimal | Yes | Number of units (> 0.01) | 1104.97 |
| nav | BigDecimal | Yes | NAV per unit (> 0.01) | 45.25 |
| status | String | Yes | Transaction status | "COMPLETED" |

**Calculation Formula:**
```
units = amount / nav
```

Example:
- amount = 50000.00
- nav = 45.25
- units = 50000.00 / 45.25 = 1104.97

**cURL Example:**
```bash
curl --request POST \
  --url 'http://localhost:9095/transactions' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json' \
  --data '{
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-15",
    "fundName": "Axis Bluechip Fund",
    "type": "BUY",
    "amount": 50000.00,
    "units": 1104.97,
    "nav": 45.25,
    "status": "COMPLETED"
  }'
```

**Expected Response (201 CREATED):**
```json
{
  "transaction": {
    "id": "770e8400-e29b-41d4-a716-446655440003",
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-15",
    "fundName": "Axis Bluechip Fund",
    "type": "BUY",
    "amount": 50000.00,
    "units": 1104.97,
    "nav": 45.25,
    "status": "COMPLETED"
  }
}
```

**HTTP Status Codes:**
- `201 CREATED` - Transaction created successfully
- `400 BAD REQUEST` - Validation error (see error messages below)
- `401 UNAUTHORIZED` - Missing or invalid token
- `404 NOT FOUND` - User or Fund not found

---

### 5. Create Transaction (SELL)

Create a SELL transaction for a mutual fund.

**Method:** POST

**Endpoint:**
```
{{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (RAW JSON):**
```json
{
  "fundId": "660e8400-e29b-41d4-a716-446655440001",
  "date": "2024-03-20",
  "fundName": "Axis Bluechip Fund",
  "type": "SELL",
  "amount": 48500.00,
  "units": 1072.00,
  "nav": 45.20,
  "status": "COMPLETED"
}
```

**Key Differences from BUY:**
- `type` is "SELL" instead of "BUY"
- The amount received from selling units at current NAV
- Units to sell should not exceed units bought in previous BUY transactions

**cURL Example:**
```bash
curl --request POST \
  --url 'http://localhost:9095/transactions' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json' \
  --data '{
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-20",
    "fundName": "Axis Bluechip Fund",
    "type": "SELL",
    "amount": 48500.00,
    "units": 1072.00,
    "nav": 45.20,
    "status": "COMPLETED"
  }'
```

**Expected Response (201 CREATED):**
```json
{
  "transaction": {
    "id": "770e8400-e29b-41d4-a716-446655440004",
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-20",
    "fundName": "Axis Bluechip Fund",
    "type": "SELL",
    "amount": 48500.00,
    "units": 1072.00,
    "nav": 45.20,
    "status": "COMPLETED"
  }
}
```

---

### 6. Create Transaction (DIVIDEND)

Create a DIVIDEND transaction when dividends are received.

**Method:** POST

**Endpoint:**
```
{{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (RAW JSON):**
```json
{
  "fundId": "660e8400-e29b-41d4-a716-446655440001",
  "date": "2024-03-25",
  "fundName": "Axis Bluechip Fund",
  "type": "DIVIDEND",
  "amount": 250.00,
  "units": 5.53,
  "nav": 45.25,
  "status": "COMPLETED"
}
```

**Key Differences from BUY/SELL:**
- `type` is "DIVIDEND"
- Dividend amount is typically smaller
- Represents reinvestment of dividends as additional units

**cURL Example:**
```bash
curl --request POST \
  --url 'http://localhost:9095/transactions' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE' \
  --header 'Content-Type: application/json' \
  --data '{
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-25",
    "fundName": "Axis Bluechip Fund",
    "type": "DIVIDEND",
    "amount": 250.00,
    "units": 5.53,
    "nav": 45.25,
    "status": "COMPLETED"
  }'
```

**Expected Response (201 CREATED):**
```json
{
  "transaction": {
    "id": "770e8400-e29b-41d4-a716-446655440005",
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-25",
    "fundName": "Axis Bluechip Fund",
    "type": "DIVIDEND",
    "amount": 250.00,
    "units": 5.53,
    "nav": 45.25,
    "status": "COMPLETED"
  }
}
```

---

### 7. Get All Transactions (After Creation)

Retrieve all transactions after creating a new transaction.

**Method:** GET

**Endpoint:**
```
{{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440005",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-25",
      "fundName": "Axis Bluechip Fund",
      "type": "DIVIDEND",
      "amount": 250.00,
      "units": 5.53,
      "nav": 45.25,
      "status": "COMPLETED"
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440004",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-20",
      "fundName": "Axis Bluechip Fund",
      "type": "SELL",
      "amount": 48500.00,
      "units": 1072.00,
      "nav": 45.20,
      "status": "COMPLETED"
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440003",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-15",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 50000.00,
      "units": 1104.97,
      "nav": 45.25,
      "status": "COMPLETED"
    }
  ]
}
```

> Note: Transactions are sorted by date in descending order (newest first) by default.

---

### 8. Filter by Type (BUY)

Get only BUY transactions.

**Method:** GET

**Endpoint:**
```
{{baseUrl}}/transactions?type=BUY
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440003",
      "fundId": "660e8400-e29b-41d4-a716-446655440001",
      "date": "2024-03-15",
      "fundName": "Axis Bluechip Fund",
      "type": "BUY",
      "amount": 50000.00,
      "units": 1104.97,
      "nav": 45.25,
      "status": "COMPLETED"
    }
  ]
}
```

---

### 9. Error Cases

#### 9.1 Missing Authorization Token

**Endpoint:**
```
GET {{baseUrl}}/transactions
```

**Headers (Missing Authorization):**
```
Content-Type: application/json
```

**Expected Response (401 UNAUTHORIZED):**
```json
{
  "timestamp": "2024-03-15T10:30:00.000000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/transactions"
}
```

---

#### 9.2 Invalid Token

**Endpoint:**
```
GET {{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer invalid_token_here
Content-Type: application/json
```

**Expected Response (401 UNAUTHORIZED):**
```json
{
  "timestamp": "2024-03-15T10:30:00.000000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/transactions"
}
```

---

#### 9.3 Missing Required Field (fundId)

**Endpoint:**
```
POST {{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (Missing fundId):**
```json
{
  "date": "2024-03-15",
  "fundName": "Axis Bluechip Fund",
  "type": "BUY",
  "amount": 50000.00,
  "units": 1104.97,
  "nav": 45.25,
  "status": "COMPLETED"
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "timestamp": "2024-03-15T10:30:00.000000",
  "status": 400,
  "error": "Bad Request",
  "message": "Fund ID is required",
  "path": "/transactions"
}
```

---

#### 9.4 Invalid Amount (Less Than Minimum)

**Endpoint:**
```
POST {{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (Amount too small):**
```json
{
  "fundId": "660e8400-e29b-41d4-a716-446655440001",
  "date": "2024-03-15",
  "fundName": "Axis Bluechip Fund",
  "type": "BUY",
  "amount": 0.001,
  "units": 0.00002,
  "nav": 45.25,
  "status": "COMPLETED"
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "timestamp": "2024-03-15T10:30:00.000000",
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be greater than 0",
  "path": "/transactions"
}
```

---

#### 9.5 Fund Not Found

**Endpoint:**
```
POST {{baseUrl}}/transactions
```

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Request Body (Non-existent fundId):**
```json
{
  "fundId": "00000000-0000-0000-0000-000000000000",
  "date": "2024-03-15",
  "fundName": "Non-Existent Fund",
  "type": "BUY",
  "amount": 50000.00,
  "units": 1104.97,
  "nav": 45.25,
  "status": "COMPLETED"
}
```

**Expected Response (404 NOT FOUND):**
```json
{
  "timestamp": "2024-03-15T10:30:00.000000",
  "status": 404,
  "error": "Not Found",
  "message": "Fund not found",
  "path": "/transactions"
}
```

---

## Testing Workflow Summary

Here's a recommended order to test all APIs:

1. **Sign Up** - Create a user account
2. **Sign In** - Get authentication token
3. **Get Funds** - Get available funds and their IDs
4. **Get All Transactions** - Should return empty (first call)
5. **Create Transaction (BUY)** - First investment
6. **Create Transaction (BUY)** - Second investment (different fund or date)
7. **Create Transaction (SELL)** - Sell some units
8. **Create Transaction (DIVIDEND)** - Dividend received
9. **Get All Transactions** - Should show all 4 transactions
10. **Get Transactions by Type (BUY)** - Filter BUY transactions
11. **Get Transactions by Type (SELL)** - Filter SELL transactions
12. **Get Transactions by Type (DIVIDEND)** - Filter DIVIDEND transactions
13. **Get Transactions (Ascending Sort)** - Oldest first
14. **Error Test Cases** - Test all error scenarios

---

## Important Notes

### Data Validation Rules

- **fundId**: Must be a valid UUID of an existing fund
- **amount**: Must be > 0.01
- **units**: Must be > 0.01
- **nav**: Must be > 0.01 and should match the fund's current NAV
- **date**: Must be in YYYY-MM-DD format
- **type**: Must be one of: BUY, SELL, DIVIDEND (case-insensitive in request, stored as uppercase)
- **status**: In this implementation, automatically set to "COMPLETED"

### Calculation Notes

```
Units Calculation:
units = amount / nav

Example:
If you invest 50,000 at NAV of 45.25:
units = 50,000 / 45.25 = 1,104.97
```

### Transaction Types

| Type | Usage | Example |
|------|-------|---------|
| BUY | Purchase of mutual fund units | Investing money to buy units |
| SELL | Sale of owned mutual fund units | Redeeming units |
| DIVIDEND | Dividend received and reinvested | Quarterly/Annual dividend |

### Database Persistence

- All transactions are persisted in PostgreSQL
- Transactions are linked to the user and fund entities
- Deleting a user will cascade delete all their transactions
- Transactions have UUIDs as unique identifiers

---

## Postman Collection JSON

If you want to import this directly into Postman, save the following as a JSON file and import it:

```json
{
  "info": {
    "name": "WealthWise - Transaction Module",
    "description": "Complete Transaction API testing collection",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Sign Up",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"name\": \"John Doe\", \"email\": \"john.doe@example.com\", \"password\": \"SecurePassword@123\"}"
        },
        "url": {
          "raw": "{{baseUrl}}/users/auth/sign-up",
          "host": ["{{baseUrl}}"],
          "path": ["users", "auth", "sign-up"]
        }
      }
    },
    {
      "name": "Sign In",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"john.doe@example.com\", \"password\": \"SecurePassword@123\"}"
        },
        "url": {
          "raw": "{{baseUrl}}/users/auth/sign-in",
          "host": ["{{baseUrl}}"],
          "path": ["users", "auth", "sign-in"]
        }
      }
    },
    {
      "name": "Get Funds",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/funds",
          "host": ["{{baseUrl}}"],
          "path": ["funds"]
        }
      }
    },
    {
      "name": "Get All Transactions",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/transactions",
          "host": ["{{baseUrl}}"],
          "path": ["transactions"]
        }
      }
    },
    {
      "name": "Create Transaction - BUY",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"fundId\": \"{{fundId}}\", \"date\": \"2024-03-15\", \"fundName\": \"Axis Bluechip Fund\", \"type\": \"BUY\", \"amount\": 50000.00, \"units\": 1104.97, \"nav\": 45.25, \"status\": \"COMPLETED\"}"
        },
        "url": {
          "raw": "{{baseUrl}}/transactions",
          "host": ["{{baseUrl}}"],
          "path": ["transactions"]
        }
      }
    },
    {
      "name": "Get Transactions - Filter by Type (BUY)",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/transactions?type=BUY",
          "host": ["{{baseUrl}}"],
          "path": ["transactions"],
          "query": [
            {
              "key": "type",
              "value": "BUY"
            }
          ]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:9095"
    },
    {
      "key": "token",
      "value": ""
    },
    {
      "key": "fundId",
      "value": ""
    }
  ]
}
```

---

## Troubleshooting

### Issue: "Unauthorized" Error
**Solution:** 
- Verify token is copied correctly from Sign In response
- Check token has not expired
- Ensure Authorization header format is: `Bearer <token>`

### Issue: "Fund not found"
**Solution:**
- Use a valid fundId from the /funds endpoint
- Verify fundId is a valid UUID format

### Issue: "User not found"
**Solution:**
- Ensure you're signed in and have a valid token
- Check userId in the token matches an existing user

### Issue: Bad Request (Validation Error)
**Solution:**
- Check all required fields are present
- Verify numeric values (amount, units, nav) are > 0.01
- Check amount and units are not too small
- Verify date format is YYYY-MM-DD
- Ensure type is one of: BUY, SELL, DIVIDEND

---

## Quick Test Commands

You can also test using bash/terminal with cURL:

```bash
# 1. Sign In and save token
TOKEN=$(curl -s -X POST \
  'http://localhost:9095/users/auth/sign-in' \
  -H 'Content-Type: application/json' \
  -d '{"email":"john.doe@example.com","password":"SecurePassword@123"}' | jq -r '.token')

echo "Token: $TOKEN"

# 2. Get transactions
curl -X GET \
  'http://localhost:9095/transactions' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json'

# 3. Create transaction
curl -X POST \
  'http://localhost:9095/transactions' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "fundId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2024-03-15",
    "fundName": "Axis Bluechip Fund",
    "type": "BUY",
    "amount": 50000.00,
    "units": 1104.97,
    "nav": 45.25,
    "status": "COMPLETED"
  }'
```

---

## Additional Resources

- [Postman Documentation](https://learning.postman.com/)
- [REST API Best Practices](https://restfulapi.net/)
- [HTTP Status Codes](https://httpwg.org/specs/rfc7231.html#status.codes)

