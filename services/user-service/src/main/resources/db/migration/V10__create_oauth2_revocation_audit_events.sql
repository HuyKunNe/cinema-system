CREATE TABLE oauth2_revocation_audit_events
(
    id BINARY(16) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    target_type VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    target_reference VARCHAR(200)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci
        NOT NULL,

    reason VARCHAR(50)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    actor_user_id BINARY(16) NULL,

    actor_name VARCHAR(200)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci
        NOT NULL,

    revoked_authorization_count INT NOT NULL,

    occurred_at TIMESTAMP(6) NOT NULL,

    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT pk_oauth2_revocation_audit_events
        PRIMARY KEY (id),

    CONSTRAINT chk_oauth2_revocation_audit_count
        CHECK (revoked_authorization_count >= 0)
);

CREATE INDEX idx_oauth2_revocation_audit_target
    ON oauth2_revocation_audit_events (
        target_type,
        target_reference,
        occurred_at
    );

CREATE INDEX idx_oauth2_revocation_audit_actor
    ON oauth2_revocation_audit_events (
        actor_user_id,
        occurred_at
    );

CREATE INDEX idx_oauth2_revocation_audit_reason
    ON oauth2_revocation_audit_events (
        reason,
        occurred_at
    );
