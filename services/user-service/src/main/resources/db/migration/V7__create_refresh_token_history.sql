CREATE TABLE oauth2_refresh_token_history (
    id BINARY(16) NOT NULL,

    authorization_id VARCHAR(100) NOT NULL,

    registered_client_id VARCHAR(100) NOT NULL,

    principal_name VARCHAR(200) NOT NULL,

    token_hash CHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    status VARCHAR(20) NOT NULL,

    issued_at DATETIME(6) NOT NULL,

    expires_at DATETIME(6) NOT NULL,

    rotated_at DATETIME(6) NULL,

    revoked_at DATETIME(6) NULL,

    reused_at DATETIME(6) NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL,

    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_oauth2_refresh_token_history
        PRIMARY KEY (id),

    CONSTRAINT uk_oauth2_refresh_token_history_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_oauth2_refresh_token_history_authorization
        FOREIGN KEY (authorization_id)
        REFERENCES oauth2_authorization (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_oauth2_refresh_token_history_hash
        CHECK (
            token_hash REGEXP '^[0-9a-f]{64}$'
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_status
        CHECK (
            status IN (
                'ACTIVE',
                'ROTATED',
                'REVOKED',
                'REUSED'
            )
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_time
        CHECK (
            expires_at > issued_at
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_transition_time
        CHECK (
            (
                status = 'ACTIVE'
                AND rotated_at IS NULL
                AND revoked_at IS NULL
                AND reused_at IS NULL
            )
            OR
            (
                status = 'ROTATED'
                AND rotated_at IS NOT NULL
                AND revoked_at IS NULL
                AND reused_at IS NULL
            )
            OR
            (
                status = 'REVOKED'
                AND revoked_at IS NOT NULL
                AND reused_at IS NULL
            )
            OR
            (
                status = 'REUSED'
                AND rotated_at IS NOT NULL
                AND reused_at IS NOT NULL
                AND revoked_at IS NULL
            )
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_rotated_time
        CHECK (
            rotated_at IS NULL
            OR rotated_at >= issued_at
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_revoked_time
        CHECK (
            revoked_at IS NULL
            OR revoked_at >= issued_at
        ),

    CONSTRAINT chk_oauth2_refresh_token_history_reused_time
        CHECK (
            reused_at IS NULL
            OR (
                rotated_at IS NOT NULL
                AND reused_at >= rotated_at
            )
        )
);

CREATE INDEX idx_oauth2_refresh_token_history_authorization
    ON oauth2_refresh_token_history (
        authorization_id
    );

CREATE INDEX idx_oauth2_refresh_token_history_client_principal
    ON oauth2_refresh_token_history (
        registered_client_id,
        principal_name
    );

CREATE INDEX idx_oauth2_refresh_token_history_status_expires
    ON oauth2_refresh_token_history (
        status,
        expires_at
    );
