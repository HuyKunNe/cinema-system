CREATE TABLE pricing_quotes (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    showtime_id BINARY(16) NOT NULL,
    showtime_version BIGINT NOT NULL,
    policy_id BINARY(16) NOT NULL,
    policy_version BIGINT NOT NULL,

    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    snapshot_json LONGTEXT NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_quotes PRIMARY KEY (id),

    CONSTRAINT fk_pricing_quotes_showtime
        FOREIGN KEY (showtime_id)
        REFERENCES showtimes (id),

    CONSTRAINT fk_pricing_quotes_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT chk_pricing_quotes_versions
        CHECK (showtime_version >= 0 AND policy_version > 0),

    CONSTRAINT chk_pricing_quotes_expiration
        CHECK (expires_at > issued_at)
);

CREATE INDEX idx_pricing_quotes_user_issued
    ON pricing_quotes (user_id, issued_at);

CREATE INDEX idx_pricing_quotes_expiration
    ON pricing_quotes (expires_at);
