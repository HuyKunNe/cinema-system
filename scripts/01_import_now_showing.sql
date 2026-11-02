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

-- Official listing source: https://www.cgv.vn/default/movies/now-showing.html/
-- Trailer source: https://www.cgv.vn/home
-- Gundam rating: detail page T16 used instead of conflicting cached listing T18.
INSERT INTO tmp_cgv_seed_stage
    (id, title, description, duration_minutes, release_date, age_rating, status, trailer_url, genre_names)
VALUES
    (UUID_TO_BIN('1b4db471-dc64-5492-a6e2-9fed3582b55e'), 'Phim Điện Ảnh Doraemon: Nobita và Lâu Đài Dưới Đáy Biển (Phiên bản mới)', NULL, 101, '2026-05-22', 'P', 'NOW_SHOWING', 'https://www.youtube.com/watch?v=u3JgYkmuK78', '["Hoạt Hình", "Phiêu Lưu"]'),
    (UUID_TO_BIN('20244093-7c27-5c79-97a7-d255a084f6d5'), 'TẠM BIỆT GOHAN', NULL, 140, '2026-05-15', 'K', 'NOW_SHOWING', 'https://www.youtube.com/watch?v=PaGtIdi8ONk', '["Gia đình"]'),
    (UUID_TO_BIN('eda573a2-2935-5546-aa80-ba58da50559a'), 'STAR WARS: MANDALORIAN VÀ GROGU', NULL, 132, '2026-05-22', 'T13', 'NOW_SHOWING', 'https://www.youtube.com/watch?v=Rwtlb0QtJxA', '["Hành Động", "Phiêu Lưu"]'),
    (UUID_TO_BIN('03da36dd-7998-5759-ad29-302a7645a083'), 'MA DA HÀN QUỐC: HỒ NUỐT NGƯỜI', NULL, 95, '2026-05-15', 'T18', 'NOW_SHOWING', 'https://www.youtube.com/watch?v=T1fnpd8Icv8', '["Kinh Dị"]'),
    (UUID_TO_BIN('e7ac8842-d5b5-5431-89dc-4fb4500ff194'), 'THE SHEEP DETECTIVES', NULL, 109, '2026-05-08', 'K', 'NOW_SHOWING', 'https://www.youtube.com/watch?v=aGd6qOTplko', '["Bí ẩn", "Hài", "Hành Động"]'),
    (UUID_TO_BIN('dcd86299-22c2-5d75-a9bc-162934b89ff1'), 'PHÍ PHÔNG: QUỶ MÁU RỪNG THIÊNG', NULL, 120, '2026-04-24', 'T16', 'NOW_SHOWING', NULL, '["Hồi hộp", "Kinh Dị"]'),
    (UUID_TO_BIN('0543e887-8016-58ce-9b47-d414f6f45cec'), 'LÀNG KHÁT MÁU', NULL, 103, '2026-05-22', 'T18', 'NOW_SHOWING', NULL, '["Kinh Dị"]'),
    (UUID_TO_BIN('4247a29a-9aa1-5cb6-ab2d-90fdfc05a365'), 'MỘT THỜI TA ĐÃ YÊU', 'Bảo trải qua mùa hè tuổi mười tám tại một thị trấn biển, nơi mối tình đầu để lại những ký ức kéo dài nhiều năm.', 119, '2026-05-15', 'T16', 'NOW_SHOWING', NULL, '["Tâm Lý", "Tình cảm"]'),
    (UUID_TO_BIN('4532c39a-e1ad-5efd-b565-09dfff8eb3c2'), 'MOBILE SUIT GUNDAM: TIA CHỚP HATHAWAY', 'Hathaway bí mật lãnh đạo Mafty chống lại Liên bang Trái Đất trong khi lực lượng của Kenneth truy lùng tổ chức.', 95, '2026-05-15', 'T16', 'NOW_SHOWING', NULL, '["Hoạt Hình"]'),
    (UUID_TO_BIN('c74eb570-7338-57ac-b020-d0c69759d843'), 'MICHAEL', NULL, 127, '2026-04-22', 'K', 'NOW_SHOWING', NULL, '["Nhạc kịch", "Tâm Lý"]');

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
