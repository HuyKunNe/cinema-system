ALTER TABLE showtimes
    ADD COLUMN movie_screening_version_id BINARY(16) NULL,
    ADD COLUMN movie_screening_version_revision BIGINT NULL,
    ADD COLUMN content_format VARCHAR(20) NULL,
    ADD COLUMN audio_language VARCHAR(100) NULL,
    ADD COLUMN presentation_type VARCHAR(30) NULL,
    ADD COLUMN subtitle_language VARCHAR(100) NULL,

    ADD CONSTRAINT chk_showtimes_screening_snapshot
    CHECK (
        (
            movie_screening_version_id IS NULL
            AND movie_screening_version_revision IS NULL
            AND content_format IS NULL
            AND audio_language IS NULL
            AND presentation_type IS NULL
            AND subtitle_language IS NULL
        )
        OR
        (
            movie_screening_version_id IS NOT NULL
            AND movie_screening_version_revision IS NOT NULL
            AND movie_screening_version_revision >= 0
            AND content_format IS NOT NULL
            AND content_format IN ('TWO_D', 'THREE_D')
            AND audio_language IS NOT NULL
            AND CHAR_LENGTH(TRIM(audio_language)) > 0
            AND presentation_type IS NOT NULL
            AND (
                (
                    presentation_type = 'SUBTITLED'
                    AND subtitle_language IS NOT NULL
                    AND CHAR_LENGTH(TRIM(subtitle_language)) > 0
                )
                OR
                (
                    presentation_type IN ('DUBBED', 'NO_SUBTITLES')
                    AND subtitle_language IS NULL
                )
            )
        )
    );

CREATE INDEX idx_showtimes_screening_version_start
    ON showtimes (movie_screening_version_id, starts_at);
