ALTER TABLE showtimes
    ADD COLUMN pricing_policy_id BINARY(16) NULL,
    ADD COLUMN pricing_policy_version BIGINT NULL,
    ADD COLUMN experience_type VARCHAR(30) NULL,
    ADD COLUMN content_format VARCHAR(10) NULL,
    ADD COLUMN official_release_date DATE NULL,
    ADD COLUMN movie_version BIGINT NULL,
    ADD COLUMN room_layout_version BIGINT NULL,

    ADD CONSTRAINT fk_showtimes_pricing_policy
        FOREIGN KEY (pricing_policy_id)
        REFERENCES cinema_pricing_policies (id),

    ADD CONSTRAINT chk_showtimes_pricing_snapshot
        CHECK (
            (
                pricing_policy_id IS NULL
                AND pricing_policy_version IS NULL
                AND experience_type IS NULL
                AND content_format IS NULL
                AND official_release_date IS NULL
                AND movie_version IS NULL
                AND room_layout_version IS NULL
            )
            OR
            (
                pricing_policy_id IS NOT NULL
                AND pricing_policy_version > 0
                AND experience_type IS NOT NULL
                AND experience_type IN (
                    'STANDARD', 'PREMIUM_CINEMA', 'FOUR_DX', 'IMAX'
                )
                AND content_format IS NOT NULL
                AND content_format IN ('TWO_D', 'THREE_D')
                AND official_release_date IS NOT NULL
                AND movie_version IS NOT NULL
                AND movie_version >= 0
                AND room_layout_id IS NOT NULL
                AND room_layout_version IS NOT NULL
                AND room_layout_version > 0
            )
        );

CREATE INDEX idx_showtimes_pricing_policy
    ON showtimes (pricing_policy_id);

ALTER TABLE show_seats
    MODIFY COLUMN price DECIMAL(12, 2) NULL;
