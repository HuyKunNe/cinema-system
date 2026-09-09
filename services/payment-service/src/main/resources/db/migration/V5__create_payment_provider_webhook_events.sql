CREATE TABLE payment_provider_webhook_events (
    id BINARY(16) NOT NULL,
    payment_transaction_id BINARY(16) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    provider_reference VARCHAR(255) NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_payment_provider_webhook_events
        PRIMARY KEY (id),

    CONSTRAINT fk_payment_provider_webhook_events_transaction
        FOREIGN KEY (payment_transaction_id)
        REFERENCES payment_transactions (id),

    CONSTRAINT uk_payment_provider_webhook_events_provider_event
        UNIQUE (
            provider,
            provider_event_id
        ),

    CONSTRAINT chk_payment_provider_webhook_events_outcome
        CHECK (
            outcome IN (
                'SUCCEEDED',
                'FAILED',
                'PENDING',
                'UNKNOWN'
            )
        ),

    CONSTRAINT chk_payment_provider_webhook_events_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payment_provider_webhook_events_currency
        CHECK (
            REGEXP_LIKE(
                currency,
                '^[A-Z]{3}$',
                'c'
            )
        )
);

CREATE INDEX idx_payment_provider_webhook_events_transaction
    ON payment_provider_webhook_events (
        payment_transaction_id,
        processed_at
    );
