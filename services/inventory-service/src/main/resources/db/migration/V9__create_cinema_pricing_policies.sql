CREATE TABLE cinema_pricing_policies (
    id BINARY(16) NOT NULL,
    cinema_id BINARY(16) NOT NULL,
    policy_version BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    published_at DATETIME(6) NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_cinema_pricing_policies
        PRIMARY KEY (id),

    CONSTRAINT fk_pricing_policies_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id),

    CONSTRAINT uk_pricing_policies_cinema_version
        UNIQUE (cinema_id, policy_version),

    CONSTRAINT chk_pricing_policies_positive_version
        CHECK (policy_version > 0),

    CONSTRAINT chk_pricing_policies_period
        CHECK (
            effective_to IS NULL
            OR effective_to > effective_from
        ),

    CONSTRAINT chk_pricing_policies_publication
        CHECK (
            (status = 'DRAFT' AND published_at IS NULL)
            OR
            (status = 'PUBLISHED' AND published_at IS NOT NULL)
        )
);

CREATE INDEX idx_pricing_policies_cinema_status_period
    ON cinema_pricing_policies (
        cinema_id,
        status,
        effective_from,
        effective_to
    );
