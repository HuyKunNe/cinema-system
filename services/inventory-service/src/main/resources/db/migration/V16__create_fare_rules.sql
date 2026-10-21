ALTER TABLE pricing_day_groups
    ADD CONSTRAINT uk_pricing_day_groups_policy_id
        UNIQUE (policy_id, id);

ALTER TABLE pricing_time_bands
    ADD CONSTRAINT uk_pricing_time_bands_policy_id
        UNIQUE (policy_id, id);

CREATE TABLE fare_rules (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    experience_type VARCHAR(30) NOT NULL,
    content_format VARCHAR(20) NOT NULL,

    day_group_id BINARY(16) NOT NULL,
    time_band_id BINARY(16) NOT NULL,

    ticket_category_code VARCHAR(64) NOT NULL,
    seat_type_code VARCHAR(64) NOT NULL,

    first_showing_condition VARCHAR(30) NOT NULL,
    required_eligibility_code VARCHAR(64) NULL,

    amount_vnd BIGINT NOT NULL,
    fare_unit VARCHAR(20) NOT NULL,
    priority INT NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_fare_rules
        PRIMARY KEY (id),

    CONSTRAINT fk_fare_rules_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT fk_fare_rules_day_group_policy
        FOREIGN KEY (policy_id, day_group_id)
        REFERENCES pricing_day_groups (policy_id, id),

    CONSTRAINT fk_fare_rules_time_band_policy
        FOREIGN KEY (policy_id, time_band_id)
        REFERENCES pricing_time_bands (policy_id, id),

    CONSTRAINT uk_fare_rules_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT chk_fare_rules_experience
        CHECK (
            experience_type IN (
                'STANDARD',
                'PREMIUM_CINEMA',
                'FOUR_DX',
                'IMAX'
            )
        ),

    CONSTRAINT chk_fare_rules_content_format
        CHECK (content_format IN ('TWO_D', 'THREE_D')),

    CONSTRAINT chk_fare_rules_first_showing
        CHECK (
            first_showing_condition IN (
                'ANY',
                'FIRST_SHOWING',
                'NOT_FIRST_SHOWING'
            )
        ),

    CONSTRAINT chk_fare_rules_unit
        CHECK (
            fare_unit IN (
                'PER_TICKET',
                'PER_PAIR',
                'UNCONFIRMED'
            )
        ),

    CONSTRAINT chk_fare_rules_amount
        CHECK (amount_vnd >= 0),

    CONSTRAINT chk_fare_rules_priority
        CHECK (priority >= 0),

    CONSTRAINT chk_fare_rules_period
        CHECK (
            effective_to IS NULL
            OR effective_to > effective_from
        )
);

CREATE INDEX idx_fare_rules_policy_priority
    ON fare_rules (policy_id, priority);
