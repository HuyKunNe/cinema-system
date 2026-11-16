ALTER TABLE bookings
    ADD COLUMN showtime_end_source VARCHAR(40) NULL,
    ADD COLUMN showtime_end_recorded_at DATETIME(6) NULL,
    ADD COLUMN showtime_end_recorded_by VARCHAR(100) NULL;

-- Existing non-null values were populated through reservation snapshots.
UPDATE bookings
SET showtime_end_source = 'RESERVATION_EVENT'
WHERE showtime_ends_at IS NOT NULL AND showtime_end_source IS NULL;
