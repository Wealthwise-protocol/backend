-- Payment Module Migration Script
-- Creates the payments table for handling SIP and installment payments

-- Enable UUID extension (required for uuid_generate_v4())
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    external_payment_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    CONSTRAINT fk_payments_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'NET_BANKING', 'WALLET', 'UPI'))
);

-- Create index on user_id for faster queries
CREATE INDEX idx_payments_user_id ON payments(user_id);

-- Create index on status for filtering
CREATE INDEX idx_payments_status ON payments(status);

-- Create index on created_at for sorting
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- Update transactions table to support FAILED status if migration is progressive
-- The table should already have this, but ensure the enum/check constraint exists
-- This is a comment documenting the expected status values for transactions:
-- COMPLETED - Payment was successful, units were allocated
-- FAILED - Payment failed, no units were allocated (only amount is recorded for audit)
-- PENDING - Payment is pending (optional for future use)
