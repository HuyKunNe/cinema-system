CREATE TABLE security_audit_events
(
    id BINARY(16) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    event_type VARCHAR(60)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    actor_type VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    actor_reference VARCHAR(200)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci
        NOT NULL,

    target_type VARCHAR(30)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NULL,

    target_reference VARCHAR(200)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci
        NULL,

    outcome VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    correlation_id VARCHAR(100)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NULL,

    reason VARCHAR(100)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NULL,

    metadata VARCHAR(1000)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci
        NULL,

    occurred_at TIMESTAMP(6) NOT NULL,

    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT pk_security_audit_events
        PRIMARY KEY (id),

    CONSTRAINT chk_security_audit_target_pair
        CHECK (
            (
                target_type IS NULL
                AND target_reference IS NULL
            )
            OR
            (
                target_type IS NOT NULL
                AND target_reference IS NOT NULL
            )
        )
);

CREATE INDEX idx_security_audit_event_time
    ON security_audit_events (
        event_type,
        occurred_at
    );

CREATE INDEX idx_security_audit_actor
    ON security_audit_events (
        actor_type,
        actor_reference,
        occurred_at
    );

CREATE INDEX idx_security_audit_target
    ON security_audit_events (
        target_type,
        target_reference,
        occurred_at
    );

CREATE INDEX idx_security_audit_outcome
    ON security_audit_events (
        outcome,
        occurred_at
    );

CREATE INDEX idx_security_audit_correlation
    ON security_audit_events (
        correlation_id
    );
