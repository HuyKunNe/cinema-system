ALTER TABLE showtimes
    ADD COLUMN projection_technology VARCHAR(30) NULL AFTER content_format;

UPDATE showtimes
SET projection_technology = 'STANDARD'
WHERE movie_screening_version_id IS NOT NULL
  AND projection_technology IS NULL;

ALTER TABLE showtimes
    DROP CHECK chk_showtimes_screening_snapshot,
    ADD CONSTRAINT chk_showtimes_screening_snapshot CHECK (
        (
            movie_screening_version_id IS NULL
            AND movie_screening_version_revision IS NULL
            AND content_format IS NULL
            AND projection_technology IS NULL
            AND audio_language IS NULL
            AND presentation_type IS NULL
            AND subtitle_language IS NULL
        )
        OR
        (
            movie_screening_version_id IS NOT NULL
            AND movie_screening_version_revision IS NOT NULL
            AND movie_screening_version_revision >= 0
            AND content_format IN ('TWO_D', 'THREE_D')
            AND projection_technology IN ('STANDARD', 'IMAX', 'FOUR_DX', 'SCREEN_X')
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
