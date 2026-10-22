CREATE TABLE movie_screening_formats (
    movie_id BINARY(16) NOT NULL,
    screening_format VARCHAR(40) NOT NULL,

    CONSTRAINT pk_movie_screening_formats
        PRIMARY KEY (movie_id, screening_format),

    CONSTRAINT fk_movie_screening_formats_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_movie_screening_formats_value
        CHECK (
            screening_format IN (
                'STANDARD_2D',
                'STANDARD_3D',
                'PREMIUM_CINEMA_2D',
                'PREMIUM_CINEMA_3D',
                'FOUR_DX_2D',
                'FOUR_DX_3D',
                'IMAX_2D',
                'IMAX_3D'
            )
        )
);
