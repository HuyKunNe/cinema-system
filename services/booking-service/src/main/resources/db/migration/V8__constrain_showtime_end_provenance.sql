-- Reject partial provenance without rewriting historical values.
ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_showtime_end_provenance CHECK (
        (showtime_ends_at IS NULL
            AND showtime_end_source IS NULL
            AND showtime_end_recorded_at IS NULL
            AND showtime_end_recorded_by IS NULL)
        OR
        (showtime_ends_at IS NOT NULL
            AND showtime_end_source IS NOT NULL
            AND showtime_end_source = 'RESERVATION_EVENT'
            AND showtime_end_recorded_at IS NULL
            AND showtime_end_recorded_by IS NULL)
        OR
        (showtime_ends_at IS NOT NULL
            AND showtime_end_source IS NOT NULL
            AND showtime_end_source = 'CURRENT_SCHEDULE_BACKFILL'
            AND showtime_end_recorded_at IS NOT NULL
            AND showtime_end_recorded_by IS NOT NULL
            AND REGEXP_LIKE(showtime_end_recorded_by,
                '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'))
    );
