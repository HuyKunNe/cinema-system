CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    correlation_id BINARY(16) NULL,
    causation_id BINARY(16) NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    last_error VARCHAR(2000) NULL,
    processing_owner VARCHAR(150) NULL,
    processing_started_at DATETIME(6) NULL,
    processing_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,

    CONSTRAINT pk_outbox_events
        PRIMARY KEY (id),

    CONSTRAINT chk_outbox_events_retry_count
        CHECK (retry_count >= 0),

    CONSTRAINT chk_outbox_events_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_outbox_events_processing_lease
        CHECK (
            (
                status = 'PROCESSING'
                AND processing_owner IS NOT NULL
                AND processing_started_at IS NOT NULL
                AND processing_expires_at IS NOT NULL
            )
            OR
            (
                status <> 'PROCESSING'
                AND processing_owner IS NULL
                AND processing_started_at IS NULL
                AND processing_expires_at IS NULL
            )
        ),

    CONSTRAINT chk_outbox_events_publication
        CHECK (
            (
                status = 'SENT'
                AND published_at IS NOT NULL
                AND next_attempt_at IS NULL
            )
            OR
            (
                status <> 'SENT'
                AND published_at IS NULL
            )
        )
);

CREATE INDEX idx_outbox_events_claim
    ON outbox_events (
        status,
        next_attempt_at,
        processing_expires_at,
        created_at
    );

CREATE INDEX idx_outbox_events_processing_owner
    ON outbox_events (
        processing_owner,
        status
    );

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (
        aggregate_type,
        aggregate_id
    );

CREATE INDEX idx_outbox_events_correlation
    ON outbox_events (correlation_id);
