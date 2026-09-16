CREATE TABLE financial_audit_records
(
    id             BINARY(16)   NOT NULL,
    payment_id     BINARY(16)   NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    actor_type     VARCHAR(50)  NOT NULL,
    actor_id       VARCHAR(255) NOT NULL,
    reason         VARCHAR(500) NULL,
    metadata       VARCHAR(2000) NULL,
    correlation_id BINARY(16)   NOT NULL,
    occurred_at    DATETIME(6)  NOT NULL,

    CONSTRAINT pk_financial_audit_records
        PRIMARY KEY (id),

    CONSTRAINT fk_financial_audit_records_payment
        FOREIGN KEY (payment_id)
            REFERENCES payments (id),

    CONSTRAINT chk_financial_audit_records_action
        CHECK (
            action IN (
                'REFUND_REQUESTED',
                'REFUND_SUCCEEDED',
                'REFUND_FAILED',
                'RECONCILIATION_OPENED',
                'RECONCILIATION_RESOLVED',
                'RECONCILIATION_REJECTED'
            )
        ),

    CONSTRAINT chk_financial_audit_records_actor_type
        CHECK (
            actor_type IN (
                'USER',
                'SERVICE',
                'SYSTEM'
            )
        ),

    CONSTRAINT chk_financial_audit_records_actor_id
        CHECK (CHAR_LENGTH(TRIM(actor_id)) > 0)
);

CREATE INDEX idx_financial_audit_payment_occurred
    ON financial_audit_records (payment_id, occurred_at);

CREATE INDEX idx_financial_audit_actor_occurred
    ON financial_audit_records (actor_type, actor_id, occurred_at);

CREATE INDEX idx_financial_audit_action_occurred
    ON financial_audit_records (action, occurred_at);
