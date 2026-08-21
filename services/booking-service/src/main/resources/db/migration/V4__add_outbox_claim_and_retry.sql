ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at DATETIME(6) NULL
        AFTER retry_count,
    ADD COLUMN last_error VARCHAR(2000) NULL
        AFTER next_attempt_at,
    ADD COLUMN processing_owner VARCHAR(150) NULL
        AFTER last_error,
    ADD COLUMN processing_started_at DATETIME(6) NULL
        AFTER processing_owner,
    ADD COLUMN processing_expires_at DATETIME(6) NULL
        AFTER processing_started_at;

UPDATE outbox_events
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL
  AND status IN (
      'PENDING',
      'FAILED'
  );

ALTER TABLE outbox_events
    DROP INDEX idx_outbox_events_publish;

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
