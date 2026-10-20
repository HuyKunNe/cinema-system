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
