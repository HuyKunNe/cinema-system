-- Historical bookings retain NULL until an authoritative Inventory synchronization exists.
-- Do not derive the screening end from the booking hold expiration.
ALTER TABLE bookings
    ADD COLUMN showtime_ends_at DATETIME(6) NULL;

CREATE INDEX idx_bookings_user_status_showtime_end
    ON bookings (user_id, status, showtime_ends_at, id);
