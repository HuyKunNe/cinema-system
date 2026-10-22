ALTER TABLE pricing_quotes
    ADD COLUMN consumed_by_booking_id BINARY(16) NULL,
    ADD COLUMN consumed_at DATETIME(6) NULL,
    ADD COLUMN reservation_expires_at DATETIME(6) NULL,

    ADD CONSTRAINT uk_pricing_quotes_consumed_booking
        UNIQUE (consumed_by_booking_id),

    ADD CONSTRAINT chk_pricing_quotes_consumption
        CHECK (
            (
                consumed_by_booking_id IS NULL
                AND consumed_at IS NULL
                AND reservation_expires_at IS NULL
            )
            OR
            (
                consumed_by_booking_id IS NOT NULL
                AND consumed_at IS NOT NULL
                AND reservation_expires_at IS NOT NULL
                AND consumed_at >= issued_at
                AND consumed_at < expires_at
                AND reservation_expires_at > consumed_at
                AND reservation_expires_at <= expires_at
            )
        );
