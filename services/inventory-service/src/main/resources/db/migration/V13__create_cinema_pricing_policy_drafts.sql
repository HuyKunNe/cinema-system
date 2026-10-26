CREATE TABLE cinema_pricing_policies (
    id BINARY(16) NOT NULL,
    cinema_id BINARY(16) NOT NULL,
    client_request_id BINARY(16) NOT NULL,
    creation_fingerprint VARCHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_cinema_pricing_policies
        PRIMARY KEY (id),

    CONSTRAINT fk_cinema_pricing_policies_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id),

    CONSTRAINT uk_cinema_pricing_policies_request
        UNIQUE (cinema_id, client_request_id)
);

CREATE TABLE cinema_pricing_policy_versions (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,
    revision_number BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    day_group VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_cinema_pricing_policy_versions
        PRIMARY KEY (id),

    CONSTRAINT fk_cinema_pricing_policy_versions_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_cinema_pricing_policy_versions_revision
        UNIQUE (policy_id, revision_number),

    CONSTRAINT chk_cinema_pricing_policy_revision
        CHECK (revision_number >= 1),

    CONSTRAINT chk_cinema_pricing_policy_day_group
        CHECK (day_group IN ('WEEKDAY', 'WEEKEND', 'HOLIDAY')),

    CONSTRAINT chk_cinema_pricing_policy_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED'))
);
