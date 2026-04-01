# Payment Module - Testing Guide

This guide provides step-by-step instructions to test the Payment Module using Postman or curl.

## Prerequisites

1. API running on `http://localhost:8080`
2. User authenticated and have valid JWT token
3. At least one Fund exists in the system
4. Postman or curl installed

## Variables Setup

Set these variables in your Postman environment:

```
base_url: http://localhost:8080
auth_token: <Your JWT Token>
user_id: <Your User UUID>
fund_id: <Fund UUID>
payment_id: <Payment UUID (will be filled during tests)>
sip_id: <SIP UUID (will be filled during tests)>
installment_id: <Installment UUID (will be filled during tests)>
```

## Test Scenarios

### 1. Payment Module Tests

#### 1.1 Create Payment (Initial/SIP Creation)
```
POST http://localhost:8080/payments
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "amount": 5000,
  "paymentMethod": "CREDIT_CARD",
  "description": "SIP Initial Investment - HDFC Mid Cap Fund",
  "externalPaymentId": "stripe_txn_12345"
}
```

**Expected Response (201 Created):**
```json
{
  "payment": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "amount": 5000,
    "paymentMethod": "CREDIT_CARD",
    "status": "PENDING",
    "description": "SIP Initial Investment - HDFC Mid Cap Fund",
    "externalPaymentId": "stripe_txn_12345",
    "createdAt": "2024-01-15T10:30:00",
    "processedAt": null,
    "failureReason": null
  }
}
```

Save the `payment.id` as `{{payment_id}}`

---

#### 1.2 Get All Payments
```
GET http://localhost:8080/payments
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
```json
{
  "payments": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "amount": 5000,
      "paymentMethod": "CREDIT_CARD",
      "status": "PENDING",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

#### 1.3 Get Payment by Status
```
GET http://localhost:8080/payments/by-status?status=PENDING
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
Returns all PENDING payments

---

#### 1.4 Get Single Payment
```
GET http://localhost:8080/payments/{{payment_id}}
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
```json
{
  "payment": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "amount": 5000,
    "paymentMethod": "CREDIT_CARD",
    "status": "PENDING",
    "description": "SIP Initial Investment - HDFC Mid Cap Fund",
    "externalPaymentId": "stripe_txn_12345",
    "createdAt": "2024-01-15T10:30:00",
    "processedAt": null,
    "failureReason": null
  }
}
```

---

### 2. SIP With Payment Tests

#### 2.1 Create SIP (with payment validation)
```
POST http://localhost:8080/sips
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "fundId": "{{fund_id}}",
  "monthlyAmt": 5000,
  "paymentId": "{{payment_id}}"
}
```

**Expected Response (201 Created):**
```json
{
  "sip": {
    "id": "sip-uuid-12345",
    "fundName": "HDFC Mid Cap Fund",
    "monthlyAmt": 5000,
    "startDate": "2024-01-15",
    "nextDebit": "2024-02-15",
    "totalInvested": 5000,
    "currentValue": 5000,
    "status": "ACTIVE"
  }
}
```

Save the `sip.id` as `{{sip_id}}`

**What happens behind the scenes:**
1. ✅ Payment validated (PENDING status)
2. ✅ Payment amount matches SIP amount
3. ✅ SIP created
4. ✅ Payment status updated to SUCCESS
5. ✅ COMPLETED transaction created
6. ✅ Portfolio holding updated
7. ✅ First monthly installment created for 2024-02-15

---

#### 2.2 View SIPs
```
GET http://localhost:8080/sips
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
```json
{
  "sips": [
    {
      "id": "sip-uuid-12345",
      "fundName": "HDFC Mid Cap Fund",
      "monthlyAmt": 5000,
      "startDate": "2024-01-15",
      "nextDebit": "2024-02-15",
      "totalInvested": 5000,
      "currentValue": 5000,
      "status": "ACTIVE"
    }
  ]
}
```

---

#### 2.3 View Transaction (Completed from SIP Creation)
```
GET http://localhost:8080/transactions?sort=DESC
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
```json
{
  "transactions": [
    {
      "id": "txn-uuid-12345",
      "fundName": "HDFC Mid Cap Fund",
      "amount": 5000,
      "units": 128.21,
      "nav": 39.00,
      "status": "COMPLETED",
      "type": "BUY",
      "date": "2024-01-15"
    }
  ]
}
```

---

### 3. Installment Payment Tests

#### 3.1 Create Payment for Installment
```
POST http://localhost:8080/payments
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "amount": 5000,
  "paymentMethod": "NET_BANKING",
  "description": "SIP Monthly Installment - February 2024",
  "externalPaymentId": "stripe_txn_54321"
}
```

**Response:** Get the payment ID and save as `{{installment_payment_id}}`

---

#### 3.2 Get Installments for SIP
```
GET http://localhost:8080/sips/{{sip_id}}/installments
Authorization: Bearer {{auth_token}}
```

**Expected Response (200 OK):**
```json
{
  "installments": [
    {
      "id": "inst-uuid-001",
      "installmentDate": "2024-02-15",
      "amount": 5000,
      "status": "PENDING"
    }
  ]
}
```

Save the installment ID as `{{installment_id}}`

---

#### 3.3 Process Successful Installment Payment
```
POST http://localhost:8080/payments/process-installment
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "paymentId": "{{installment_payment_id}}",
  "sipId": "{{sip_id}}",
  "sipInstallmentId": "{{installment_id}}"
}?isSuccess=true
```

**Expected Response (200 OK):**
```json
{
  "payment": {
    "id": "{{installment_payment_id}}",
    "amount": 5000,
    "paymentMethod": "NET_BANKING",
    "status": "SUCCESS",
    "processedAt": "2024-02-15T10:30:00",
    "failureReason": null
  }
}
```

**What happens:**
1. ✅ Payment status updated to SUCCESS
2. ✅ Installment status updated to COMPLETED
3. ✅ COMPLETED transaction created
4. ✅ SIP total invested increased
5. ✅ Next debit date updated to 2024-03-15

---

#### 3.4 Process Failed Installment Payment
```
POST http://localhost:8080/payments/process-installment
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "paymentId": "{{installment_payment_id}}",
  "sipId": "{{sip_id}}",
  "sipInstallmentId": "{{installment_id}}"
}?isSuccess=false&failureReason=Insufficient%20Balance
```

**Expected Response (200 OK):**
```json
{
  "payment": {
    "id": "{{installment_payment_id}}",
    "amount": 5000,
    "paymentMethod": "NET_BANKING",
    "status": "FAILED",
    "processedAt": "2024-02-15T10:30:00",
    "failureReason": "Insufficient Balance"
  }
}
```

**What happens:**
1. ✅ Payment status updated to FAILED
2. ✅ Failure reason recorded
3. ✅ Installment status updated to FAILED
4. ✅ FAILED transaction created (0 units, 0 NAV)
5. ⚠️ Next debit date NOT updated (can retry)

---

### 4. Error Scenarios

#### 4.1 Create SIP Without Payment
```
POST http://localhost:8080/sips
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "fundId": "{{fund_id}}",
  "monthlyAmt": 5000
}
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment ID is required to create SIP",
  "path": "/sips"
}
```

---

#### 4.2 Create SIP with Non-Existent Payment
```
POST http://localhost:8080/sips
Authorization: Bearer {{auth_token}}
Content-Type: application/json

{
  "fundId": "{{fund_id}}",
  "monthlyAmt": 5000,
  "paymentId": "invalid-uuid"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid or non-existent payment",
  "path": "/sips"
}
```

---

#### 4.3 Create SIP with Wrong Payment Amount
```
POST http://localhost:8080/payments (Create payment for ₹3000)
POST http://localhost:8080/sips (Try to create SIP with monthlyAmt=5000)
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment amount must match the monthly SIP amount",
  "path": "/sips"
}
```

---

#### 4.4 Reuse Payment for Multiple SIPs
```
Create Payment → Create SIP1 (Success) → Try Create SIP2 (with same payment)
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment must be in PENDING status. Current status: SUCCESS",
  "path": "/sips"
}
```

---

## Curl Command Examples

### Create Payment
```bash
curl -X POST http://localhost:8080/payments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "paymentMethod": "CREDIT_CARD",
    "description": "SIP Initial Investment"
  }'
```

### Create SIP
```bash
curl -X POST http://localhost:8080/sips \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fundId": "YOUR_FUND_UUID",
    "monthlyAmt": 5000,
    "paymentId": "YOUR_PAYMENT_UUID"
  }'
```

### Process Installment Payment
```bash
curl -X POST "http://localhost:8080/payments/process-installment?isSuccess=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "YOUR_PAYMENT_UUID",
    "sipId": "YOUR_SIP_UUID",
    "sipInstallmentId": "YOUR_INSTALLMENT_UUID"
  }'
```

---

## Test Checklist

- [ ] Payment created successfully
- [ ] Payment appears in get-all
- [ ] Payment can be filtered by status
- [ ] SIP created without payment (fails with error)
- [ ] SIP created with valid payment (succeeds)
- [ ] Transaction created and marked COMPLETED
- [ ] Portfolio holding updated
- [ ] Next installment created
- [ ] Installment payment processed successfully
- [ ] Failed installment payment recorded
- [ ] Next debit date NOT updated on failure
- [ ] Payment status updated to SUCCESS/FAILED appropriately
- [ ] Error messages are clear and helpful

---

## Dashboard View (After Tests)

### Assets Created
```
User Dashboard:
- SIP Active: 1 (HDFC Mid Cap Fund, ₹5000/month)
- Total Invested: ₹5000
- Transactions: 1 COMPLETED (SIP Creation)
- Holdings: 128.21 units @ ₹39.00 NAV
```

### Payment History
```
Payment 1: ₹5000 | CREDIT_CARD | SUCCESS | SIP Creation
Payment 2: ₹5000 | NET_BANKING | SUCCESS | Installment 1
```

### Transaction History
```
Transaction 1: ₹5000 | HDFC Mid Cap Fund | 128.21 units | COMPLETED | 2024-01-15
Transaction 2: ₹5000 | HDFC Mid Cap Fund | 128.21 units | COMPLETED | 2024-02-15
```

---

## Notes

- Payment must be in PENDING status before creating SIP
- Payment amount must exactly match SIP monthly amount
- Only one SIP can be created per payment
- Failed installments don't block future installments
- All transactions are immutable (for audit trail)
- Payment ID is external payment gateway ID (e.g., Stripe, Razorpay)
