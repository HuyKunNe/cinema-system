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
