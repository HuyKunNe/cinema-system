ALTER TABLE showtimes
    ADD COLUMN schedule_movie_revision BIGINT NULL,
    ADD COLUMN movie_duration_minutes INT NULL,
    ADD COLUMN break_minutes INT NULL,
    ADD COLUMN ready_at DATETIME(6) NULL,

    ADD CONSTRAINT chk_showtimes_scheduling_details
    CHECK (
        (
            schedule_movie_revision IS NULL
            AND movie_duration_minutes IS NULL
            AND break_minutes IS NULL
            AND ready_at IS NULL
        )
        OR
        (
            schedule_movie_revision IS NOT NULL
            AND schedule_movie_revision >= 0
            AND movie_duration_minutes IS NOT NULL
            AND movie_duration_minutes > 0
            AND break_minutes IS NOT NULL
            AND break_minutes BETWEEN 0 AND 120
            AND ready_at IS NOT NULL
            AND ready_at >= ends_at
        )
    ),

    DROP INDEX uk_showtimes_room_start,
    ADD INDEX idx_showtimes_room_start (room_id, starts_at);
