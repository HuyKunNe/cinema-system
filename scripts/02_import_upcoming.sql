-- CGV STATIC SAMPLE: historical indexed catalog, approximately May 2026.
-- Prepared 2026-11-02. NOT a statement of today's CGV schedule.
-- MySQL 8.0.16+. Select the movie-service database before running.
-- Run the complete file in ONE connection; stop on any SQL error and ROLLBACK.
-- No Python, CGV network request or cgv_movie_imports table is required.
-- Existing movies matched by title OR seed UUID are skipped entirely.
-- No UPDATE, DELETE, REPLACE, TRUNCATE or foreign-key disabling is used.
-- Optional unavailable metadata is NULL. Hero settings use database defaults.

USE cinema_movie_db;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_cgv_seed_candidates;
DROP TEMPORARY TABLE IF EXISTS tmp_cgv_seed_stage;
CREATE TEMPORARY TABLE tmp_cgv_seed_stage (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    age_rating VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    trailer_url VARCHAR(500) NULL,
    genre_names JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Official listing source: https://www.cgv.vn/en/movies/coming-soon-1.html
-- Details: https://www.cgv.vn/en/passenger.html
-- https://www.cgv.vn/en/pee-nak-5.html
-- https://www.cgv.vn/en/hokum.html
INSERT INTO tmp_cgv_seed_stage
    (id, title, description, duration_minutes, release_date, age_rating, status, trailer_url, genre_names)
VALUES
    (UUID_TO_BIN('dbebeda6-0d31-5e76-824e-882510e56b70'), 'KHÁCH', 'Sau khi chứng kiến một tai nạn trên đường cao tốc, một đôi trẻ phát hiện một thực thể đáng sợ đang bám theo mình.', 94, '2026-05-29', 'T16', 'UPCOMING', NULL, '["Kinh Dị"]'),
    (UUID_TO_BIN('b644b027-8a38-541b-9005-1427940a11d3'), 'NGÔI ĐỀN KỲ QUÁI 5', 'Balloon, First và những người bạn trở lại ngôi đền bí ẩn để tìm cách phá lời nguyền nhắm vào chú tiểu Nott cùng gia đình.', 118, '2026-05-29', 'T16', 'UPCOMING', NULL, '["Hài", "Kinh Dị"]'),
    (UUID_TO_BIN('db2f7500-3277-599e-99e0-d76b4b8f227b'), 'BA TRỢN', 'Một nhà văn đến quán trọ ở Ireland để rải tro cốt cha mẹ và gặp những hiện tượng liên quan tới lời đồn ma ám.', 102, '2026-05-29', 'T18', 'UPCOMING', NULL, '["Kinh Dị"]');

CREATE TEMPORARY TABLE tmp_cgv_seed_candidates LIKE tmp_cgv_seed_stage;

START TRANSACTION;

INSERT INTO tmp_cgv_seed_candidates
SELECT s.*
FROM tmp_cgv_seed_stage AS s
WHERE NOT EXISTS (
    SELECT 1 FROM movies AS m WHERE m.id = s.id OR m.title = s.title
);

SET @cgv_candidates = ROW_COUNT();

-- Match existing genres by name, preserving their IDs and descriptions.
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), n.genre_name, NULL, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (
    SELECT DISTINCT j.genre_name
    FROM tmp_cgv_seed_candidates AS c
    JOIN JSON_TABLE(c.genre_names, '$[*]' COLUMNS (
        genre_name VARCHAR(100) PATH '$'
    )) AS j ON TRUE
) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM genres AS g
    WHERE g.name = n.genre_name COLLATE utf8mb4_0900_ai_ci
);

SET @cgv_genres_added = ROW_COUNT();

INSERT INTO movies (
    id, title, description, duration_minutes, release_date,
    poster_url, trailer_url, backdrop_url, age_rating, status,
    version, created_at, updated_at
)
SELECT id, title, description, duration_minutes, release_date,
       NULL, trailer_url, NULL, age_rating, status,
       0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM tmp_cgv_seed_candidates;

SET @cgv_movies_added = ROW_COUNT();

-- Link genres ONLY for the movies inserted by this execution.
INSERT INTO movie_genres (movie_id, genre_id)
SELECT c.id, g.id
FROM tmp_cgv_seed_candidates AS c
JOIN JSON_TABLE(c.genre_names, '$[*]' COLUMNS (
    genre_name VARCHAR(100) PATH '$'
)) AS j ON TRUE
JOIN genres AS g ON g.name = j.genre_name COLLATE utf8mb4_0900_ai_ci;

SET @cgv_links_added = ROW_COUNT();

COMMIT;

SELECT @cgv_movies_added AS movies_added,
       (SELECT COUNT(*) FROM tmp_cgv_seed_stage) - @cgv_candidates AS movies_skipped,
       @cgv_genres_added AS genres_added,
       @cgv_links_added AS genre_links_added;

DROP TEMPORARY TABLE tmp_cgv_seed_candidates;
DROP TEMPORARY TABLE tmp_cgv_seed_stage;
