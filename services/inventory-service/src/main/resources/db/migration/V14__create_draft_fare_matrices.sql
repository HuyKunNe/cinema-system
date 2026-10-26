ALTER TABLE cinema_pricing_policy_versions
    ADD COLUMN fare_matrix_revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE pricing_fare_scopes (
    policy_version_id BINARY(16) NOT NULL,
    experience_type VARCHAR(30) NOT NULL,
    content_format VARCHAR(20) NOT NULL,

    PRIMARY KEY (policy_version_id, experience_type, content_format),

    CONSTRAINT fk_pricing_fare_scopes_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT chk_pricing_fare_scope_experience
        CHECK (
            experience_type IN (
                'STANDARD', 'PREMIUM_CINEMA', 'IMAX',
                'FOUR_DX', 'SCREEN_X'
            )
        ),

    CONSTRAINT chk_pricing_fare_scope_format
        CHECK (content_format IN ('TWO_D', 'THREE_D'))
);

CREATE TABLE pricing_fare_categories (
    policy_version_id BINARY(16) NOT NULL,
    ticket_category VARCHAR(30) NOT NULL,

    PRIMARY KEY (policy_version_id, ticket_category),

    CONSTRAINT fk_pricing_fare_categories_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT chk_pricing_fare_category
        CHECK (
            ticket_category IN (
                'ADULT', 'SENIOR_55_PLUS', 'STUDENT_U22', 'CHILD'
            )
        )
);

CREATE TABLE pricing_fare_seat_types (
    policy_version_id BINARY(16) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,

    PRIMARY KEY (policy_version_id, seat_type),

    CONSTRAINT fk_pricing_fare_seat_types_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT chk_pricing_fare_seat_type
        CHECK (seat_type IN ('STANDARD', 'VIP', 'COUPLE', 'ACCESSIBLE'))
);

CREATE TABLE pricing_fare_cells (
    id BINARY(16) NOT NULL,
    policy_version_id BINARY(16) NOT NULL,
    experience_type VARCHAR(30) NOT NULL,
    content_format VARCHAR(20) NOT NULL,
    time_band VARCHAR(20) NOT NULL,
    ticket_category VARCHAR(30) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    state VARCHAR(24) NOT NULL,
    amount DECIMAL(12, 2) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_pricing_fare_cell
        UNIQUE (
            policy_version_id,
            experience_type,
            content_format,
            time_band,
            ticket_category,
            seat_type
        ),

    CONSTRAINT fk_pricing_fare_cells_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT fk_pricing_fare_cells_scope
        FOREIGN KEY (
            policy_version_id, experience_type, content_format
        )
        REFERENCES pricing_fare_scopes (
            policy_version_id, experience_type, content_format
        ),

    CONSTRAINT fk_pricing_fare_cells_category
        FOREIGN KEY (policy_version_id, ticket_category)
        REFERENCES pricing_fare_categories (
            policy_version_id, ticket_category
        ),

    CONSTRAINT fk_pricing_fare_cells_seat_type
        FOREIGN KEY (policy_version_id, seat_type)
        REFERENCES pricing_fare_seat_types (
            policy_version_id, seat_type
        ),

    CONSTRAINT chk_pricing_fare_time_band
        CHECK (time_band IN ('BEFORE_17', 'FROM_17')),

    CONSTRAINT chk_pricing_fare_cell_amount
        CHECK (
            (
                state = 'PRICED'
                AND amount IS NOT NULL
                AND amount >= 0
            )
            OR
            (
                state = 'NOT_APPLICABLE'
                AND amount IS NULL
            )
        )
);
