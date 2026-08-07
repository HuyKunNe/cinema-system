CREATE TABLE email_verification_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash CHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_email_verification_tokens
        PRIMARY KEY (id),

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT uk_email_verification_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_email_verification_token_hash
        CHECK (
            token_hash REGEXP '^[0-9a-f]{64}$'
        ),

    CONSTRAINT chk_email_verification_token_expiration
        CHECK (expires_at > created_at),

    CONSTRAINT chk_email_verification_token_terminal_state
        CHECK (
            used_at IS NULL
            OR revoked_at IS NULL
        )
);

CREATE INDEX idx_email_verification_tokens_user_active
    ON email_verification_tokens (
        user_id,
        used_at,
        revoked_at,
        expires_at
    );

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens (expires_at);
