CREATE TABLE payments (
    id BINARY(16) NOT NULL,
    booking_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    payment_attempt INT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    refund_status VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(255) NULL,
    failure_code VARCHAR(100) NULL,
    failure_message VARCHAR(500) NULL,
    hold_expires_at DATETIME(6) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    source_event_id BINARY(16) NOT NULL,
    correlation_id BINARY(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_payments
        PRIMARY KEY (id),

    CONSTRAINT uk_payments_booking_attempt
        UNIQUE (
            booking_id,
            payment_attempt
        ),

    CONSTRAINT uk_payments_source_event
        UNIQUE (source_event_id),

    CONSTRAINT uk_payments_provider_reference
        UNIQUE (
            provider,
            provider_reference
        ),

    CONSTRAINT chk_payments_attempt
        CHECK (payment_attempt > 0),

    CONSTRAINT chk_payments_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payments_currency
        CHECK (
            REGEXP_LIKE(
                currency,
                '^[A-Z]{3}$',
                'c'
            )
        ),

    CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                'RECEIVED',
                'PROCESSING',
                'PENDING_PROVIDER',
                'SUCCEEDED',
                'FAILED',
                'EXPIRED',
                'RECONCILIATION_REQUIRED'
            )
        ),

    CONSTRAINT chk_payments_refund_status
        CHECK (
            refund_status IN (
                'NOT_REQUESTED',
                'PENDING',
                'SUCCEEDED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_payments_hold_window
        CHECK (
            hold_expires_at > requested_at
        ),

    CONSTRAINT chk_payments_completion
        CHECK (
            completed_at IS NULL
            OR completed_at >= requested_at
        )
);

CREATE INDEX idx_payments_booking
    ON payments (booking_id);

CREATE INDEX idx_payments_user_created
    ON payments (
        user_id,
        created_at
    );

CREATE INDEX idx_payments_status_hold_expiration
    ON payments (
        status,
        hold_expires_at
    );

CREATE TABLE payment_transactions (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    attempt_number INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    provider_reference VARCHAR(255) NULL,
    provider_event_id VARCHAR(255) NULL,
    failure_code VARCHAR(100) NULL,
    failure_message VARCHAR(500) NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    processing_owner VARCHAR(150) NULL,
    processing_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_payment_transactions
        PRIMARY KEY (id),

    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT uk_payment_transactions_provider_idempotency
        UNIQUE (
            provider,
            idempotency_key
        ),

    CONSTRAINT uk_payment_transactions_provider_event
        UNIQUE (
            provider,
            provider_event_id
        ),

    CONSTRAINT uk_payment_transactions_provider_reference
        UNIQUE (
            provider,
            provider_reference
        ),

    CONSTRAINT chk_payment_transactions_attempt
        CHECK (attempt_number > 0),

    CONSTRAINT chk_payment_transactions_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payment_transactions_currency
        CHECK (
            REGEXP_LIKE(
                currency,
                '^[A-Z]{3}$',
                'c'
            )
        ),

    CONSTRAINT chk_payment_transactions_type
        CHECK (
            transaction_type IN (
                'CHARGE',
                'REFUND',
                'RECONCILIATION'
            )
        ),

    CONSTRAINT chk_payment_transactions_status
        CHECK (
            status IN (
                'READY',
                'PROCESSING',
                'PENDING_PROVIDER',
                'SUCCEEDED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_payment_transactions_processing_lease
        CHECK (
            (
                status = 'PROCESSING'
                AND processing_owner IS NOT NULL
                AND processing_expires_at IS NOT NULL
            )
            OR
            (
                status <> 'PROCESSING'
                AND processing_owner IS NULL
                AND processing_expires_at IS NULL
            )
        ),

    CONSTRAINT chk_payment_transactions_completion
        CHECK (
            (
                status IN (
                    'SUCCEEDED',
                    'FAILED'
                )
                AND completed_at IS NOT NULL
            )
            OR
            (
                status NOT IN (
                    'SUCCEEDED',
                    'FAILED'
                )
                AND completed_at IS NULL
            )
        )
);

CREATE INDEX idx_payment_transactions_payment
    ON payment_transactions (payment_id);

CREATE INDEX idx_payment_transactions_claim
    ON payment_transactions (
        status,
        processing_expires_at,
        requested_at
    );
