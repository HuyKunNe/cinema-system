CREATE TABLE pricing_sale_scopes (
    id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,

    code VARCHAR(64) NOT NULL,
    experience_type VARCHAR(30) NOT NULL,
    content_format VARCHAR(20) NOT NULL,

    day_group_code VARCHAR(64) NOT NULL,
    time_band_code VARCHAR(64) NOT NULL,
    ticket_category_code VARCHAR(64) NOT NULL,
    seat_type_code VARCHAR(64) NOT NULL,
    advance_screening_condition VARCHAR(30) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_pricing_sale_scopes PRIMARY KEY (id),

    CONSTRAINT fk_pricing_sale_scopes_policy
        FOREIGN KEY (policy_id)
        REFERENCES cinema_pricing_policies (id),

    CONSTRAINT uk_pricing_sale_scopes_policy_code
        UNIQUE (policy_id, code),

    CONSTRAINT uk_pricing_sale_scopes_dimensions
        UNIQUE (
            policy_id,
            experience_type,
            content_format,
            day_group_code,
            time_band_code,
            ticket_category_code,
            seat_type_code,
            advance_screening_condition
        ),

    CONSTRAINT chk_pricing_sale_scopes_experience
        CHECK (
            experience_type IN (
                'STANDARD', 'PREMIUM_CINEMA', 'FOUR_DX', 'IMAX'
            )
        ),

    CONSTRAINT chk_pricing_sale_scopes_content
        CHECK (content_format IN ('TWO_D', 'THREE_D')),

    CONSTRAINT chk_pricing_sale_scopes_screening
        CHECK (
            advance_screening_condition IN (
                'ANY', 'ADVANCE_SCREENING', 'REGULAR_SCREENING'
            )
        )
);
