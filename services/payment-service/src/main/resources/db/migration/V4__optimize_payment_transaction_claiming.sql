DROP INDEX idx_payment_transactions_claim
    ON payment_transactions;

CREATE INDEX idx_payment_transactions_claim
    ON payment_transactions (
        status,
        processing_expires_at,
        requested_at,
        id
    );

CREATE INDEX idx_payment_transactions_ready_claim
    ON payment_transactions (
        status,
        requested_at,
        id
    );
