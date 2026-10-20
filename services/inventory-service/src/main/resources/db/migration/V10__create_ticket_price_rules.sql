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
