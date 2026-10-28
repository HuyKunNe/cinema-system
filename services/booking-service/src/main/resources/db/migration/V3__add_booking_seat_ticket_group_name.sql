ALTER TABLE booking_seats
    ADD COLUMN ticket_group_name VARCHAR(120) NULL,

    ADD CONSTRAINT chk_booking_seats_ticket_group_name
        CHECK (
            ticket_group_name IS NULL
            OR (
                ticket_group_id IS NOT NULL
                AND inventory_seat_id IS NOT NULL
                AND CHAR_LENGTH(TRIM(ticket_group_name)) > 0
            )
        );
