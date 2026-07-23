CREATE TABLE genres (
    id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_genres PRIMARY KEY (id),
    CONSTRAINT uk_genres_name UNIQUE (name)
);

CREATE TABLE movies (
    id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NULL,
    poster_url VARCHAR(500) NULL,
    trailer_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_movies PRIMARY KEY (id),
    CONSTRAINT uk_movies_title UNIQUE (title),
    CONSTRAINT chk_movies_duration
        CHECK (duration_minutes > 0)
);

CREATE TABLE movie_genres (
    movie_id BINARY(16) NOT NULL,
    genre_id BINARY(16) NOT NULL,

    CONSTRAINT pk_movie_genres
        PRIMARY KEY (movie_id, genre_id),

    CONSTRAINT fk_movie_genres_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_movie_genres_genre
        FOREIGN KEY (genre_id)
        REFERENCES genres (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_movies_status
    ON movies (status);

CREATE INDEX idx_movies_release_date
    ON movies (release_date);

CREATE INDEX idx_movie_genres_genre_id
    ON movie_genres (genre_id);
