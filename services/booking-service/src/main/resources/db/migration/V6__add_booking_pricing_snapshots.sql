ALTER TABLE bookings
    ADD COLUMN pricing_quote_id BINARY(16) NULL,
    ADD COLUMN pricing_quote_snapshot_json LONGTEXT NULL,

    ADD CONSTRAINT uk_bookings_pricing_quote
        UNIQUE (pricing_quote_id),

    ADD CONSTRAINT chk_bookings_pricing_snapshot
        CHECK (
            pricing_quote_snapshot_json IS NULL
            OR pricing_quote_id IS NOT NULL
        );

ALTER TABLE booking_seats
    ADD COLUMN pricing_snapshot_json LONGTEXT NULL;
