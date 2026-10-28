ALTER TABLE booking_seats
    ADD COLUMN requested_ticket_group_id BINARY(16) NULL,
    ADD COLUMN ticket_group_id BINARY(16) NULL,

    ADD CONSTRAINT chk_booking_seats_group_snapshot
        CHECK (
            ticket_group_id IS NULL
            OR (
                inventory_seat_id IS NOT NULL
                AND seat_type IS NOT NULL
                AND price IS NOT NULL
            )
        ),

    ADD CONSTRAINT chk_booking_seats_requested_group
        CHECK (
            requested_ticket_group_id IS NULL
            OR inventory_seat_id IS NULL
            OR (
                ticket_group_id IS NOT NULL
                AND ticket_group_id = requested_ticket_group_id
            )
        );
