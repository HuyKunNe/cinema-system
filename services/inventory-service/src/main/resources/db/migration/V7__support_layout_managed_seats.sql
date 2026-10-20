ALTER TABLE seats
    ADD COLUMN layout_managed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN legacy_seat_number VARCHAR(20)
        GENERATED ALWAYS AS (
            CASE
                WHEN layout_managed = FALSE THEN seat_number
                ELSE NULL
            END
        ) STORED,
    DROP INDEX uk_seats_room_number,
    ADD CONSTRAINT uk_seats_room_legacy_number
        UNIQUE (room_id, legacy_seat_number);
