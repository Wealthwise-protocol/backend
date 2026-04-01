# Payment Module Documentation

## Overview

The Payment Module provides a complete payment processing system for SIP creation and monthly installments. It ensures that transactions are only recorded after successful payment, and also records failed payment attempts for audit purposes.

## Key Features

### 1. **Payment Entity**
- **UUID id**: Unique payment identifier
- **user_id**: Reference to the user making the payment
- **amount**: Payment amount
- **paymentMethod**: Type of payment (CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, UPI)
- **status**: Payment status (PENDING, SUCCESS, FAILED)
- **externalPaymentId**: Payment gateway transaction ID for integration with third-party payment providers
- **createdAt**: Payment creation timestamp
- **processedAt**: Payment processing timestamp
- **failureReason**: Reason for payment failure (if applicable)

### 2. **Payment Workflow**

#### Step 1: Create Payment
```json
POST /payments
{
  "amount": 5000,
  "paymentMethod": "CREDIT_CARD",
  "description": "SIP Initial Investment",
  "externalPaymentId": "txn_12345" // Optional, from payment gateway
}

Response:
{
  "payment": {
    "id": "uuid",
    "amount": 5000,
    "paymentMethod": "CREDIT_CARD",
    "status": "PENDING",
    "createdAt": "2024-01-15T10:30:00",
    "processedAt": null,
    "failureReason": null
  }
}
```

#### Step 2: Create SIP (with payment validation)
```json
POST /sips
{
  "fundId": "fund-uuid",
  "monthlyAmt": 5000,
  "paymentId": "payment-uuid" // Required
}

Process:
1. Validates payment exists and belongs to user
2. Validates payment status is PENDING
3. Validates payment amount matches monthlyAmt
4. Creates SIP record
5. Processes payment (SUCCESS)
6. Creates COMPLETED transaction
7. Updates portfolio holdings
8. Creates first monthly installment

Response:
{
  "sip": {
    "id": "sip-uuid",
    "fundName": "HDFC Mid Cap Fund",
    "monthlyAmt": 5000,
    "startDate": "2024-01-15",
    "nextDebit": "2024-02-15",
    "totalInvested": 5000,
    "status": "ACTIVE"
  }
}
```

### 3. **SIP Installment Payment Workflow**

#### For Successful Payment:
```json
POST /payments/process-installment
{
  "paymentId": "payment-uuid",
  "sipId": "sip-uuid",
  "sipInstallmentId": "installment-uuid"
}?isSuccess=true

Process:
1. Validates payment exists
2. Updates payment status to SUCCESS
3. Updates installment status to COMPLETED
4. Creates COMPLETED transaction
5. Updates SIP total invested
6. Updates next debit date
```

#### For Failed Payment:
```json
POST /payments/process-installment
{
  "paymentId": "payment-uuid",
  "sipId": "sip-uuid",
  "sipInstallmentId": "installment-uuid"
}?isSuccess=false&failureReason=Insufficient%20Balance

Process:
1. Updates payment status to FAILED
2. Records failure reason
3. Updates installment status to FAILED
4. Creates FAILED transaction (no units allocated)
5. Next installment can be retried
```

#### For No Payment Provided:
```
When installment date arrives and no payment is provided:
1. PaymentService.recordFailedInstallmentPayment() is called
2. Updates installment status to FAILED
3. Creates FAILED transaction for audit
4. User can retry with a new payment later
```

### 4. **Transaction Handling**

#### Transaction Statuses:
- **COMPLETED**: Payment was successful, units were allocated to user's portfolio
  ```
  {
    "id": "txn-uuid",
    "fundName": "HDFC Mid Cap Fund",
    "amount": 5000,
    "units": 128.21,
    "nav": 39.00,
    "status": "COMPLETED",
    "type": "BUY",
    "date": "2024-01-15"
  }
  ```

- **FAILED**: Payment failed, no units allocated (recorded for audit trail)
  ```
  {
    "id": "txn-uuid",
    "fundName": "HDFC Mid Cap Fund",
    "amount": 5000,
    "units": 0.00,
    "nav": 0.00,
    "status": "FAILED",
    "type": "BUY",
    "date": "2024-02-15"
  }
  ```

## API Endpoints

### Payment Management

#### Create Payment
```
POST /payments
Content-Type: application/json

{
  "amount": 10000,
  "paymentMethod": "NET_BANKING",
  "description": "SIP Monthly Installment",
  "externalPaymentId": "stripe_txn_12345"
}
```

#### Get All Payments
```
GET /payments
```

#### Get Payments by Status
```
GET /payments/by-status?status=SUCCESS
GET /payments/by-status?status=FAILED
GET /payments/by-status?status=PENDING
```

#### Get Single Payment
```
GET /payments/{paymentId}
```

#### Process Payment for SIP Creation
```
POST /payments/process-sip-creation
Content-Type: application/json

{
  "paymentId": "payment-uuid",
  "sipId": "sip-uuid"
}?isSuccess=true
```

#### Process Payment for Installment
```
POST /payments/process-installment
Content-Type: application/json

{
  "paymentId": "payment-uuid",
  "sipId": "sip-uuid",
  "sipInstallmentId": "installment-uuid"
}?isSuccess=true&failureReason=Optional%20Reason
```

### SIP Management (Updated)

#### Create SIP (Now requires payment)
```
POST /sips
Content-Type: application/json

{
  "fundId": "fund-uuid",
  "monthlyAmt": 5000,
  "paymentId": "payment-uuid"
}
```

## Integration with Payment Gateway

### Example: Stripe Integration
```java
// In PaymentService or a separate PaymentGatewayService

@Transactional
public void processPaymentWithGateway(UUID paymentId, String externalPaymentId) {
    Payment payment = paymentRepository.findById(paymentId).orElseThrow();
    
    // Call Stripe API
    Charge charge = Stripe.call(externalPaymentId);
    
    if (charge.isSuccessful()) {
        payment.setStatus("SUCCESS");
        payment.setProcessedAt(LocalDateTime.now());
    } else {
        payment.setStatus("FAILED");
        payment.setFailureReason(charge.getFailureMessage());
    }
    
    paymentRepository.save(payment);
}
```

## Database Schema

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    external_payment_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_method CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'NET_BANKING', 'WALLET', 'UPI'))
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
```

## Error Handling

### Payment Creation Errors
- **UNAUTHORIZED**: User not authenticated
- **BAD_REQUEST**: Invalid payment method or amount

### SIP Creation Errors
- **UNAUTHORIZED**: User not authenticated
- **NOT_FOUND**: Fund or user not found
- **BAD_REQUEST**: 
  - Payment not found
  - Payment not in PENDING status
  - Payment amount doesn't match SIP monthly amount

### Installment Payment Errors
- **NOT_FOUND**: Payment, installment, or SIP not found
- **BAD_REQUEST**: Invalid payment status

## User Flow Example

### Scenario: Create SIP and Pay Monthly

```
1. User creates a payment for ₹5,000 (SIP Initial)
   POST /payments
   Response: paymentId = "pay_001"

2. User creates SIP with the payment
   POST /sips
   Body: fundId, monthlyAmt=5000, paymentId="pay_001"
   Result: SIP created, COMPLETED transaction created

3. System generates first installment (nextDebit = 2024-02-15)

4. On installment date, user creates payment for ₹5,000
   POST /payments
   Response: paymentId = "pay_002"

5. System processes installment payment
   POST /payments/process-installment
   Body: paymentId="pay_002", sipId, sipInstallmentId
   Result: COMPLETED transaction created

6. If user fails to pay on time:
   - Installment can be marked FAILED
   - Transaction record still created (status=FAILED)
   - User can retry with new payment later
```

## Best Practices

1. **Always Validate Payment ID**: Before creating SIP, validate payment exists and is PENDING
2. **Amount Matching**: Ensure payment amount exactly matches SIP monthly amount
3. **External Payment ID**: Store payment gateway transaction IDs for reconciliation
4. **Audit Trail**: Failed transactions are recorded for audit and debugging
5. **Idempotency**: Use payment gateway idempotency keys to prevent duplicate charges
6. **Retry Logic**: Implement retry logic for failed payments with exponential backoff
7. **Notification**: Notify user of payment success/failure via email or SMS

## Future Enhancements

1. **Webhook Integration**: Handle payment gateway webhooks for async status updates
2. **Refund Processing**: Support refunds and reversed transactions
3. **Payment Plan**: Support EMI or flexible payment plans
4. **Payment Methods**: Add more payment methods (Credit Line, Equifax, etc.)
5. **Payment Scheduling**: Automatic payment scheduling with reminder notifications
6. **Analytics**: Payment trends, failure analysis, user retention metrics
7. **Multi-Currency**: Support international payments and currency conversion
