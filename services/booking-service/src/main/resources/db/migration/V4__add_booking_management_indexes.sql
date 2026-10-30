ALTER TABLE bookings
    ADD INDEX idx_bookings_created (
        created_at,
        id
    ),
    ADD INDEX idx_bookings_status_created (
        status,
        created_at,
        id
    ),
    ADD INDEX idx_bookings_showtime_created (
        showtime_id,
        created_at,
        id
    );
