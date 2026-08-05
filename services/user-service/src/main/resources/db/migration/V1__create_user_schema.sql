CREATE TABLE users (
    id BINARY(16) NOT NULL,
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    username VARCHAR(100) NOT NULL,
    normalized_username VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at DATETIME(6) NULL,
    locked_at DATETIME(6) NULL,
    disabled_at DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_users
        PRIMARY KEY (id),

    CONSTRAINT uk_users_normalized_email
        UNIQUE (normalized_email),

    CONSTRAINT uk_users_normalized_username
        UNIQUE (normalized_username),

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'PENDING_VERIFICATION',
                'ACTIVE',
                'LOCKED',
                'DISABLED'
            )
        )
);

CREATE INDEX idx_users_status
    ON users (status);

CREATE TABLE user_profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,
    phone_number VARCHAR(32) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_user_profiles
        PRIMARY KEY (id),

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT uk_user_profiles_user
        UNIQUE (user_id)
);

CREATE TABLE user_credentials (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_hash_algorithm VARCHAR(50) NOT NULL,
    password_changed_at DATETIME(6) NOT NULL,
    failed_attempt_count INT NOT NULL DEFAULT 0,
    last_failed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_user_credentials
        PRIMARY KEY (id),

    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT uk_user_credentials_user
        UNIQUE (user_id),

    CONSTRAINT chk_user_credentials_failed_attempts
        CHECK (failed_attempt_count >= 0)
);
