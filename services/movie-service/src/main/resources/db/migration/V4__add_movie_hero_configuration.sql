ALTER TABLE movies
    ADD COLUMN hero_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN hero_priority INT NOT NULL DEFAULT 0,
    ADD COLUMN hero_starts_at DATETIME(6) NULL,
    ADD COLUMN hero_ends_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_movies_hero_priority
        CHECK (hero_priority >= 0),
    ADD CONSTRAINT chk_movies_hero_period
        CHECK (
            hero_starts_at IS NULL
            OR hero_ends_at IS NULL
            OR hero_ends_at > hero_starts_at
        );

CREATE INDEX idx_movies_hero_selection
    ON movies (hero_enabled, status, hero_priority, id);
