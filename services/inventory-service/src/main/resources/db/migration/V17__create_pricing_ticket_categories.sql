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
