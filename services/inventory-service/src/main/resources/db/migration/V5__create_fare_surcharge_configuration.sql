ALTER TABLE cinema_pricing_policies
    ADD COLUMN fare_surcharge_mode VARCHAR(30)
        NOT NULL DEFAULT 'UNCONFIRMED',
    ADD CONSTRAINT chk_pricing_policy_fare_surcharge_mode
        CHECK (
            fare_surcharge_mode IN (
                'UNCONFIRMED',
                'NONE',
                'ADD_ALL',
                'HIGHEST_PRIORITY'
            )
        );

CREATE TABLE fare_surcharge_rules (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    fare_rule_code VARCHAR(64) NOT NULL,
    movie_attribute_code VARCHAR(64) NULL,
    required_eligibility_code VARCHAR(64) NULL,

    amount_vnd BIGINT NOT NULL,
    charge_unit VARCHAR(30) NOT NULL,
    priority INT NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_fare_surcharge_rules PRIMARY KEY (id),

    CONSTRAINT fk_fare_surcharge_rules_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_fare_surcharges_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT chk_fare_surcharges_amount
        CHECK (amount_vnd >= 0),

    CONSTRAINT chk_fare_surcharges_priority
        CHECK (priority >= 0),

    CONSTRAINT chk_fare_surcharges_unit
        CHECK (
            charge_unit IN (
                'UNCONFIRMED',
                'PER_TICKET',
                'PER_PAIR',
                'PER_BOOKING'
            )
        ),

    CONSTRAINT chk_fare_surcharges_period
        CHECK (
            effective_to IS NULL
            OR effective_to > effective_from
        )
);

CREATE INDEX idx_fare_surcharges_policy_fare
    ON fare_surcharge_rules (policy_id, fare_rule_code);
