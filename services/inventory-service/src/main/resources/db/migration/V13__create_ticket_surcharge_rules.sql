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
