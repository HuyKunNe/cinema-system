ALTER TABLE movies
    ADD COLUMN backdrop_url VARCHAR(500) NULL,
    ADD COLUMN age_rating VARCHAR(10) NULL,
    ADD CONSTRAINT chk_movies_age_rating
        CHECK (
            age_rating IS NULL
            OR age_rating IN ('P', 'K', 'T13', 'T16', 'T18')
        );
