CREATE TABLE reconciliation_cases
(
    id                     BINARY(16)   NOT NULL,
    payment_id             BINARY(16)   NOT NULL,
    payment_transaction_id BINARY(16)   NOT NULL,

    status                 VARCHAR(50)  NOT NULL,
    reason                 VARCHAR(100) NOT NULL,
    resolution             VARCHAR(100) NULL,

    provider               VARCHAR(50)  NOT NULL,
    provider_reference     VARCHAR(255) NULL,

    opened_at              DATETIME(6)  NOT NULL,
    resolved_at            DATETIME(6)  NULL,

    resolved_by_type       VARCHAR(50)  NULL,
    resolved_by            VARCHAR(255) NULL,
    resolution_reason      VARCHAR(500) NULL,

    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,

    CONSTRAINT pk_reconciliation_cases
        PRIMARY KEY (id),

    CONSTRAINT fk_reconciliation_cases_payment
        FOREIGN KEY (payment_id)
            REFERENCES payments (id),

    CONSTRAINT fk_reconciliation_cases_transaction
        FOREIGN KEY (payment_transaction_id)
            REFERENCES payment_transactions (id),

    CONSTRAINT uk_reconciliation_cases_transaction
        UNIQUE (payment_transaction_id),

    CONSTRAINT chk_reconciliation_cases_status
        CHECK (
            status IN (
                'OPEN',
                'RESOLVED',
                'REJECTED'
            )
        ),

    CONSTRAINT chk_reconciliation_cases_reason
        CHECK (
            reason IN (
                'REFUND_PROVIDER_PENDING',
                'REFUND_PROVIDER_UNKNOWN'
            )
        ),

    CONSTRAINT chk_reconciliation_cases_resolution
        CHECK (
            resolution IS NULL
            OR resolution IN (
                'REFUND_SUCCEEDED',
                'REFUND_FAILED'
            )
        ),

    CONSTRAINT chk_reconciliation_cases_resolution_state
        CHECK (
            (
                status = 'OPEN'
                AND resolution IS NULL
                AND resolved_at IS NULL
                AND resolved_by_type IS NULL
                AND resolved_by IS NULL
            )
            OR
            (
                status = 'RESOLVED'
                AND resolution IS NOT NULL
                AND resolved_at IS NOT NULL
                AND resolved_by_type IS NOT NULL
                AND resolved_by IS NOT NULL
            )
            OR
            (
                status = 'REJECTED'
                AND resolution IS NULL
                AND resolved_at IS NOT NULL
                AND resolved_by_type IS NOT NULL
                AND resolved_by IS NOT NULL
            )
        ),

    CONSTRAINT chk_reconciliation_cases_resolver_type
        CHECK (
            resolved_by_type IS NULL
            OR resolved_by_type IN (
                'USER',
                'SERVICE',
                'SYSTEM'
            )
        )
);

CREATE INDEX idx_reconciliation_cases_payment
    ON reconciliation_cases (payment_id);

CREATE INDEX idx_reconciliation_cases_status_opened
    ON reconciliation_cases (status, opened_at);

CREATE INDEX idx_reconciliation_cases_provider_reference
    ON reconciliation_cases (provider, provider_reference);
