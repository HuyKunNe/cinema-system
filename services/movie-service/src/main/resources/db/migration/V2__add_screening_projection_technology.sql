ALTER TABLE movie_screening_versions
    ADD COLUMN projection_technology VARCHAR(30) NULL AFTER content_format;

UPDATE movie_screening_versions
SET projection_technology = 'STANDARD'
WHERE projection_technology IS NULL;

ALTER TABLE movie_screening_versions
    MODIFY COLUMN projection_technology VARCHAR(30) NOT NULL,
    DROP INDEX uk_movie_screening_version_identity,
    ADD CONSTRAINT uk_movie_screening_version_identity UNIQUE (
        movie_id,
        content_format,
        projection_technology,
        audio_language,
        presentation_type,
        subtitle_language
    ),
    ADD CONSTRAINT chk_movie_screening_version_projection
        CHECK (projection_technology IN ('STANDARD', 'IMAX', 'FOUR_DX', 'SCREEN_X'));
