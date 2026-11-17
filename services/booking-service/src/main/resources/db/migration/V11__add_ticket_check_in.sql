ALTER TABLE booking_tickets
    DROP CHECK chk_booking_tickets_status,
    ADD COLUMN checked_in_at DATETIME(6) NULL,
    ADD COLUMN checked_in_by BINARY(16) NULL,
    ADD COLUMN check_in_showtime_starts_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_booking_tickets_status CHECK (status IN ('ISSUED','USED')),
    ADD CONSTRAINT chk_booking_tickets_check_in CHECK (
        (status='ISSUED' AND checked_in_at IS NULL AND checked_in_by IS NULL AND check_in_showtime_starts_at IS NULL)
        OR (status='USED' AND checked_in_at IS NOT NULL AND checked_in_by IS NOT NULL
            AND check_in_showtime_starts_at IS NOT NULL
            AND checked_in_at >= DATE_SUB(check_in_showtime_starts_at, INTERVAL 30 MINUTE)
            AND checked_in_at <= DATE_ADD(check_in_showtime_starts_at, INTERVAL 15 MINUTE))
    );
