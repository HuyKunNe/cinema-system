-- Consolidated schema for fresh Inventory databases.

-- Source: V8__add_cinema_timezone.sql
ALTER TABLE cinemas
    ADD COLUMN time_zone VARCHAR(64) NULL;

-- Source: V9__create_cinema_pricing_policies.sql
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

-- Source: V10__create_ticket_price_rules.sql
CREATE TABLE ticket_price_rules (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    ticket_category VARCHAR(32) NOT NULL,
    day_type VARCHAR(20) NOT NULL,

    from_minute INT NOT NULL,
    to_minute INT NOT NULL,

    amount DECIMAL(12, 2) NOT NULL,
    priority INT NOT NULL,

    required_member_tier VARCHAR(64) NULL,
    required_channel VARCHAR(64) NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_ticket_price_rules
        PRIMARY KEY (id),

    CONSTRAINT fk_ticket_price_rules_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_ticket_price_rules_policy_code_category
        UNIQUE (policy_id, code, ticket_category),

    CONSTRAINT chk_ticket_price_rules_time_range
        CHECK (
            from_minute >= 0
            AND from_minute < 1440
            AND to_minute > from_minute
            AND to_minute <= 1440
        ),

    CONSTRAINT chk_ticket_price_rules_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_ticket_price_rules_priority
        CHECK (priority >= 0),

    CONSTRAINT chk_ticket_price_rules_day_type
        CHECK (day_type IN ('ANY', 'REGULAR', 'HOLIDAY'))
);

CREATE INDEX idx_ticket_price_rules_policy_priority
    ON ticket_price_rules (policy_id, priority);

CREATE TABLE ticket_price_rule_days (
    rule_id BINARY(16) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,

    CONSTRAINT pk_ticket_price_rule_days
        PRIMARY KEY (rule_id, day_of_week),

    CONSTRAINT fk_ticket_price_rule_days_rule
        FOREIGN KEY (rule_id)
        REFERENCES ticket_price_rules (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ticket_price_rule_days_value
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        )
);

-- Source: V11__add_pricing_policy_content_revision.sql
ALTER TABLE cinema_pricing_policies
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_pricing_policy_content_revision
        CHECK (content_revision >= 0);

-- Source: V12__create_cinema_holiday_dates.sql
CREATE TABLE cinema_holiday_dates (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,
    holiday_date DATE NOT NULL,
    name VARCHAR(150) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_cinema_holiday_dates
        PRIMARY KEY (id),

    CONSTRAINT fk_cinema_holiday_dates_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_cinema_holiday_dates_policy_date
        UNIQUE (policy_id, holiday_date)
);

-- Source: V13__create_ticket_surcharge_rules.sql
CREATE TABLE ticket_surcharge_rules (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    surcharge_target VARCHAR(32) NOT NULL,
    target_code VARCHAR(64) NOT NULL,

    amount DECIMAL(12, 2) NOT NULL,
    charge_unit VARCHAR(30) NOT NULL,
    stackable BOOLEAN NOT NULL,
    priority INT NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_ticket_surcharge_rules
        PRIMARY KEY (id),

    CONSTRAINT fk_ticket_surcharge_rules_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_ticket_surcharge_rules_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT chk_ticket_surcharge_rules_target
        CHECK (
            surcharge_target IN (
                'SEAT_TYPE',
                'MOVIE_ATTRIBUTE',
                'SHOWTIME_FORMAT'
            )
        ),

    CONSTRAINT chk_ticket_surcharge_rules_unit
        CHECK (
            charge_unit IN (
                'PER_TICKET',
                'PER_SEAT'
            )
        ),

    CONSTRAINT chk_ticket_surcharge_rules_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_ticket_surcharge_rules_priority
        CHECK (priority >= 0)
);

CREATE INDEX idx_ticket_surcharge_rules_policy_priority
    ON ticket_surcharge_rules (policy_id, priority);

-- Source: V14__add_pricing_policy_model.sql
ALTER TABLE cinema_pricing_policies
    ADD COLUMN pricing_model VARCHAR(30)
        NOT NULL DEFAULT 'BASE_RULES_V1',
    ADD CONSTRAINT chk_cinema_pricing_policy_model
        CHECK (
            pricing_model IN (
                'BASE_RULES_V1',
                'FARE_MATRIX_V2'
            )
        );

-- Source: V15__create_pricing_matrix_dimensions.sql
CREATE TABLE pricing_day_groups (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    includes_holidays BOOLEAN NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_day_groups
        PRIMARY KEY (id),

    CONSTRAINT fk_pricing_day_groups_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_pricing_day_groups_policy_code
        UNIQUE (policy_id, code)
);

CREATE TABLE pricing_day_group_days (
    day_group_id BINARY(16) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,

    CONSTRAINT pk_pricing_day_group_days
        PRIMARY KEY (day_group_id, day_of_week),

    CONSTRAINT fk_pricing_day_group_days_group
        FOREIGN KEY (day_group_id)
        REFERENCES pricing_day_groups (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_pricing_day_group_days_value
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        )
);

CREATE TABLE pricing_time_bands (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    from_minute INT NOT NULL,
    to_minute INT NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_time_bands
        PRIMARY KEY (id),

    CONSTRAINT fk_pricing_time_bands_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_pricing_time_bands_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT chk_pricing_time_bands_range
        CHECK (
            from_minute >= 0
            AND from_minute < 1440
            AND to_minute > from_minute
            AND to_minute <= 1440
        )
);

-- Source: V16__create_fare_rules.sql
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

    advance_screening_condition VARCHAR(30) NOT NULL,
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

    CONSTRAINT chk_fare_rules_advance_screening
        CHECK (
            advance_screening_condition IN (
                'ANY',
                'ADVANCE_SCREENING',
                'REGULAR_SCREENING'
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

-- Source: V17__create_pricing_ticket_categories.sql
CREATE TABLE pricing_ticket_categories (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,

    eligibility_mode VARCHAR(30) NOT NULL,
    eligibility_rule_code VARCHAR(64) NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_ticket_categories
        PRIMARY KEY (id),

    CONSTRAINT fk_pricing_ticket_categories_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_pricing_ticket_categories_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT chk_pricing_ticket_categories_eligibility
        CHECK (
            (
                eligibility_mode IN ('UNCONFIRMED', 'UNRESTRICTED')
                AND eligibility_rule_code IS NULL
            )
            OR
            (
                eligibility_mode = 'RULE_REQUIRED'
                AND eligibility_rule_code IS NOT NULL
            )
        )
);

-- Source: V18__create_pricing_seat_types.sql
CREATE TABLE pricing_seat_types (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,

    mapping_status VARCHAR(30) NOT NULL,
    physical_seat_type VARCHAR(30) NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_seat_types
        PRIMARY KEY (id),

    CONSTRAINT fk_pricing_seat_types_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_pricing_seat_types_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT uk_pricing_seat_types_policy_physical
        UNIQUE (policy_id, physical_seat_type),

    CONSTRAINT chk_pricing_seat_types_mapping
        CHECK (
            (
                mapping_status = 'UNCONFIRMED'
                AND physical_seat_type IS NULL
            )
            OR
            (
                mapping_status = 'CONFIRMED'
                AND physical_seat_type IS NOT NULL
            )
        ),

    CONSTRAINT chk_pricing_seat_types_physical
        CHECK (
            physical_seat_type IS NULL
            OR physical_seat_type IN (
                'STANDARD',
                'VIP',
                'COUPLE',
                'ACCESSIBLE'
            )
        )
);
