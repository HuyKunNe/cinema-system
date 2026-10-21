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
