CREATE TABLE processed_events (
    id BINARY(16) NOT NULL,
    event_id BINARY(16) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    processed_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_processed_events
        PRIMARY KEY (id),

    CONSTRAINT uk_processed_events_event_consumer
        UNIQUE (
            event_id,
            consumer_name
        )
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events (processed_at);

CREATE INDEX idx_processed_events_type
    ON processed_events (
        event_type,
        processed_at
    );
