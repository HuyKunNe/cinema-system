CREATE TABLE user_external_identities (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    issuer VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_external_identity_issuer_subject (issuer, subject),
    UNIQUE KEY uk_user_external_identity_user_issuer (user_id, issuer),
    KEY idx_user_external_identity_user (user_id),
    CONSTRAINT fk_user_external_identity_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pending_user_registrations (
    id BINARY(16) NOT NULL,
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,
    phone_number VARCHAR(32) NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_registration_normalized_email (normalized_email),
    UNIQUE KEY uk_pending_registration_token_hash (token_hash),
    KEY idx_pending_registration_expiration (expires_at),
    CONSTRAINT chk_pending_registration_token_hash
        CHECK (REGEXP_LIKE(token_hash, '^[0-9a-f]{64}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
