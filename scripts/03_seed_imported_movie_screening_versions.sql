-- Add a sample screening version for movies from import scripts 01 and 02.
-- Local sample configuration:
-- STANDARD / TWO_D / Ngôn ngữ gốc / NO_SUBTITLES / active.
-- This configuration is not verified CGV screening-version metadata.
--
-- MySQL 8.0.16+.
-- Run the complete file in ONE connection.
-- Stop on any SQL error and execute ROLLBACK.
--
-- Existing screening versions are preserved, including inactive versions.
-- Movie rows, genres and genre links are preserved.

USE cinema_movie_db;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_cgv_screening_targets;
DROP TEMPORARY TABLE IF EXISTS tmp_cgv_screening_seed;

CREATE TEMPORARY TABLE tmp_cgv_screening_seed (
    seed_movie_id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TEMPORARY TABLE tmp_cgv_screening_targets (
    seed_movie_id BINARY(16) NOT NULL PRIMARY KEY,
    movie_id BINARY(16) NULL,
    UNIQUE KEY uk_tmp_cgv_screening_movie (movie_id)
) ENGINE=InnoDB;

INSERT INTO tmp_cgv_screening_seed (seed_movie_id, title)
VALUES
    (
        UUID_TO_BIN('1b4db471-dc64-5492-a6e2-9fed3582b55e'),
        'Phim Điện Ảnh Doraemon: Nobita và Lâu Đài Dưới Đáy Biển (Phiên bản mới)'
    ),
    (
        UUID_TO_BIN('20244093-7c27-5c79-97a7-d255a084f6d5'),
        'TẠM BIỆT GOHAN'
    ),
    (
        UUID_TO_BIN('eda573a2-2935-5546-aa80-ba58da50559a'),
        'STAR WARS: MANDALORIAN VÀ GROGU'
    ),
    (
        UUID_TO_BIN('03da36dd-7998-5759-ad29-302a7645a083'),
        'MA DA HÀN QUỐC: HỒ NUỐT NGƯỜI'
    ),
    (
        UUID_TO_BIN('e7ac8842-d5b5-5431-89dc-4fb4500ff194'),
        'THE SHEEP DETECTIVES'
    ),
    (
        UUID_TO_BIN('dcd86299-22c2-5d75-a9bc-162934b89ff1'),
        'PHÍ PHÔNG: QUỶ MÁU RỪNG THIÊNG'
    ),
    (
        UUID_TO_BIN('0543e887-8016-58ce-9b47-d414f6f45cec'),
        'LÀNG KHÁT MÁU'
    ),
    (
        UUID_TO_BIN('4247a29a-9aa1-5cb6-ab2d-90fdfc05a365'),
        'MỘT THỜI TA ĐÃ YÊU'
    ),
    (
        UUID_TO_BIN('4532c39a-e1ad-5efd-b565-09dfff8eb3c2'),
        'MOBILE SUIT GUNDAM: TIA CHỚP HATHAWAY'
    ),
    (
        UUID_TO_BIN('c74eb570-7338-57ac-b020-d0c69759d843'),
        'MICHAEL'
    ),
    (
        UUID_TO_BIN('dbebeda6-0d31-5e76-824e-882510e56b70'),
        'KHÁCH'
    ),
    (
        UUID_TO_BIN('b644b027-8a38-541b-9005-1427940a11d3'),
        'NGÔI ĐỀN KỲ QUÁI 5'
    ),
    (
        UUID_TO_BIN('db2f7500-3277-599e-99e0-d76b4b8f227b'),
        'BA TRỢN'
    );

SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

START TRANSACTION;

-- Prefer the seed UUID.
-- Fall back to title only when that seed UUID does not exist.
-- This also handles movies skipped by the original import because
-- another movie row already had the same title.

INSERT INTO tmp_cgv_screening_targets (seed_movie_id, movie_id)
SELECT
    s.seed_movie_id,
    m.id
FROM tmp_cgv_screening_seed AS s
LEFT JOIN movies AS m
    ON m.id = s.seed_movie_id
    OR (
        m.title = s.title
        AND NOT EXISTS (
            SELECT 1
            FROM movies AS by_id
            WHERE by_id.id = s.seed_movie_id
        )
    );

-- Coordinate with MovieScreeningVersionService, which also locks
-- the owning movie before creating or changing a screening version.

SELECT BIN_TO_UUID(m.id) AS locked_movie_id
FROM movies AS m
WHERE m.id IN (
    SELECT t.movie_id
    FROM tmp_cgv_screening_targets AS t
    WHERE t.movie_id IS NOT NULL
)
ORDER BY m.id
FOR UPDATE;

INSERT INTO movie_screening_versions (
    id,
    movie_id,
    content_format,
    projection_technology,
    audio_language,
    presentation_type,
    subtitle_language,
    active,
    version,
    created_at,
    updated_at
)
SELECT
    -- UUID v7:
    -- 48-bit Unix timestamp in milliseconds,
    -- version 7, RFC variant, and random remaining bits.
    UNHEX(
        CONCAT(
            LPAD(
                HEX(
                    CAST(
                        UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000
                        AS UNSIGNED
                    )
                ),
                12,
                '0'
            ),
            '7',
            SUBSTRING(HEX(RANDOM_BYTES(2)), 2, 3),
            HEX(8 + MOD(ORD(RANDOM_BYTES(1)), 4)),
            SUBSTRING(HEX(RANDOM_BYTES(8)), 2, 15)
        )
    ),
    m.id,
    'TWO_D',
    'STANDARD',
    'Ngôn ngữ gốc',
    'NO_SUBTITLES',
    '',
    1,
    0,
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
FROM movies AS m
JOIN tmp_cgv_screening_targets AS t
    ON t.movie_id = m.id
WHERE m.status IN ('NOW_SHOWING', 'UPCOMING')
  AND NOT EXISTS (
      SELECT 1
      FROM movie_screening_versions AS existing_version
      WHERE existing_version.movie_id = m.id
  );

SET @cgv_screening_versions_added = ROW_COUNT();

COMMIT;

SELECT
    @cgv_screening_versions_added AS screening_versions_added;

SELECT
    BIN_TO_UUID(t.movie_id) AS movie_id,
    s.title AS seed_title,
    m.title AS movie_title,
    m.status AS movie_status,
    (
        SELECT COUNT(*)
        FROM movie_screening_versions AS v
        WHERE v.movie_id = m.id
          AND v.active = 1
    ) AS active_version_count,
    CASE
        WHEN m.id IS NULL THEN
            'MOVIE_NOT_FOUND'
        WHEN m.status NOT IN ('NOW_SHOWING', 'UPCOMING') THEN
            'MOVIE_NOT_SCHEDULABLE'
        WHEN EXISTS (
            SELECT 1
            FROM movie_screening_versions AS v
            WHERE v.movie_id = m.id
              AND v.active = 1
        ) THEN
            'ACTIVE_VERSION_AVAILABLE'
        WHEN EXISTS (
            SELECT 1
            FROM movie_screening_versions AS v
            WHERE v.movie_id = m.id
        ) THEN
            'EXISTING_VERSIONS_INACTIVE'
        ELSE
            'MISSING_VERSION'
    END AS result
FROM tmp_cgv_screening_seed AS s
JOIN tmp_cgv_screening_targets AS t
    ON t.seed_movie_id = s.seed_movie_id
LEFT JOIN movies AS m
    ON m.id = t.movie_id
ORDER BY s.title;

DROP TEMPORARY TABLE tmp_cgv_screening_targets;
DROP TEMPORARY TABLE tmp_cgv_screening_seed;
