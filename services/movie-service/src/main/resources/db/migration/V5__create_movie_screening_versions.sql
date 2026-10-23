CREATE TABLE movie_screening_versions (
    id BINARY(16) NOT NULL,
    movie_id BINARY(16) NOT NULL,

    content_format VARCHAR(20) NOT NULL,
    audio_language VARCHAR(100) NOT NULL,
    presentation_type VARCHAR(30) NOT NULL,
    subtitle_language VARCHAR(100) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_movie_screening_versions PRIMARY KEY (id),

    CONSTRAINT fk_movie_screening_versions_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies (id),

    CONSTRAINT uk_movie_screening_version_identity
        UNIQUE (
            movie_id,
            content_format,
            audio_language,
            presentation_type,
            subtitle_language
        ),

    CONSTRAINT chk_movie_screening_version_format
        CHECK (content_format IN ('TWO_D', 'THREE_D')),

    CONSTRAINT chk_movie_screening_version_presentation
        CHECK (
            (
                presentation_type = 'SUBTITLED'
                AND subtitle_language <> ''
            )
            OR
            (
                presentation_type IN ('DUBBED', 'NO_SUBTITLES')
                AND subtitle_language = ''
            )
        )
);
