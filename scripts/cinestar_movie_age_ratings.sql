-- Cinestar age-rating snapshot: 2026-10-05
-- Backend baseline: 2c0bf3fd134cd6bc5105ba9162eba7012170a3d0
--
-- Sources:
-- https://cinestar.com.vn/movie/showing/
-- https://cinestar.com.vn/movie/upcoming/
--
-- Internal IDs and exact titles:
-- scripts/cinestar_seed.sql at the backend baseline above.
--
-- Classification values come from source metadata, not title parsing.
-- 40 classified movies; 14 unclassified movies are not updated.
--
-- Prerequisites:
-- 1. Flyway V3__add_movie_metadata.sql has been applied.
-- 2. The original Cinestar movie seed has been applied.
-- 3. Run this file in ONE connection, stopping on any SQL error.
-- 4. If an error occurs during the transaction, execute ROLLBACK.
--
-- Change USE if MOVIE_DB_URL points to another database.

SET NAMES utf8mb4;

USE cinema_movie_db;

-- Verify required columns exist before preparing the update.
SELECT id, title, age_rating, version, updated_at
FROM movies
LIMIT 0;

DROP TEMPORARY TABLE IF EXISTS cinestar_age_rating_seed;

CREATE TEMPORARY TABLE cinestar_age_rating_seed (
    movie_id_hex CHAR(32) CHARACTER SET ascii NOT NULL,
    title VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL,
    age_rating VARCHAR(10) CHARACTER SET ascii NOT NULL,
    PRIMARY KEY (movie_id_hex)
) ENGINE = InnoDB;

INSERT INTO cinestar_age_rating_seed
    (movie_id_hex, title, age_rating)
VALUES
    ('03d1cd6dbc32579cb4ac2556c09d87b5',
     'HÒN ĐẢO QUÊN LÃNG LT (K)', 'K'),
    ('a7ba608e7331585cb577a2fccfca21a9',
     'PHÁO HOA LÚC BÌNH MINH (P)', 'P'),
    ('61e9d70d165b5f9fa5615877b6d4bb30',
     'HÒN ĐẢO QUÊN LÃNG PĐ (K)', 'K'),
    ('51ff33807d57537398d4015711f374f9',
     'QUYẾT CUA ANH NÀY! (PĐ) (T13)', 'T13'),
    ('059fc591d31f58b7a5fa86e9d8ad4176',
     'KHÓA CHẶT CỬA NÀO SUZUME (P)', 'P'),
    ('9393ae2d77a1532a8dacaa742b64c60d',
     'THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13)', 'T13'),
    ('4b1a74f4829050dd96cc21890b67e1cc',
     'QUYẾT CUA ANH NÀY! LT (T13)', 'T13'),
    ('881b3c0bfec4545fb496a0f296db6ed1',
     'SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P)', 'P'),
    ('a9d69ba3591b5d8ba4b512aa718bace2',
     'QUỶ ĂN TẠNG 4: HỔ TINH LT (T18)', 'T18'),
    ('b03297fc2c9653f4906ecd30ae373087',
     'ÁN MẠNG XÉM HOÀN HẢO (T18)', 'T18'),
    ('6b802086780a5a6fa1ba590683709c33',
     'QUỶ ĂN TẠNG 4: HỔ TINH PĐ (T18)', 'T18'),
    ('3aad75ab42e65dd581d3bc5d67a2d7f9',
     'ALWAYS LALISA', 'P'),
    ('3d3a1a25fc4256328a12b5cae9dde455',
     'ÚT LAN 2 (T18)', 'T18'),
    ('daf28a3d701c54499ae7fe3405eb5f55',
     'LÊN HƯƠNG (T16)', 'T16'),
    ('43e0179c3af554cd93f65a9099626076',
     'TRẠI BUÔN NGƯỜI (T18)', 'T18'),
    ('ae0c431510f8513eb3473c536ac70b4f',
     'BÓNG MA NHÀ HÁT (T16)', 'T16'),
    ('771713ed20035beab0068ee6bfbf4adf',
     'VÙNG ĐẤT QUỶ DỮ (T18)', 'T18'),
    ('f558db23f7635adf8c8d2b722152d8ed',
     'HỘ LINH TRÁNG SĨ (T13): BÍ ẨN MỘ VUA ĐINH (TG)', 'T13'),
    ('ea78d048566f54df8d87ea54c8b95d31',
     'HỘ LINH TRÁNG SĨ: BÍ ẨN MỘ VUA ĐINH (T13)', 'T13'),
    ('3bede48f3be35294ad1b070300517078',
     'AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13)', 'T13'),
    ('c872f64f835e5697b1f92d2b2574cc67',
     'TÀU BUÔN NGƯỜI (T18)', 'T18'),
    ('1f59b15c13de5ae4a0f4b0a5d53793db',
     'CỔ THUẬT HẮC NGẢI (T16)', 'T16'),
    ('6ee15a2d45185a21a463d420e89a0416',
     'BÙA YÊU: BÍ MẬT GIA TỘC (T13)', 'T13'),
    ('fde155d92ef450caac5187c1c9e54b38',
     'MA TÙ LT (T18)', 'T18'),
    ('f5ddf8feebff553ba6d37866dad9e9a2',
     'NGHỈ HÈ SỢ NGHỈ HƯU (T13)', 'T13'),
    ('e91d795a12865e3a81ab54fa3f7041fe',
     'HOPE VÙNG TỬ ĐỊA (T16)', 'T16'),
    ('b7b63d5a787e51bd9a8b3d51587a7547',
     'YÊU NHÂN THẦN THÁM: KỲ ÁN TRƯỜNG AN (K)', 'K'),
    ('cf8d00c7ff8b5d16a7a2e134d8db2af4',
     'BÁT TIÊN ! TRUY TÌM LƯU LY ĐĂNG (K)', 'K'),
    ('7dd7d64de5c95612814ff7204358d4ab',
     'TRÁI TIM QUÁI THÚ (T13)', 'T13'),
    ('58b56da983fe5a1ea78973e062842632',
     'PHIM SHIN – CẬU BÉ BÚT CHÌ: KỲ KỲ QUÁI QUÁI! KỲ NGHỈ YÊU QUÁI CỦA TỚ LT (P)', 'P'),
    ('d4866186b88951438d7cdf5c6951fbc2',
     'TẾ NHI CẢI MỆNH (T18)', 'T18'),
    ('acd8170f4e4452b181d8054f70ea668b',
     'NGƯỜI MẸ KHÁC (T18)', 'T18'),
    ('5881129fb661566ea736e71451f481bf',
     'CHUYỆN TÌNH KHAU VAI', 'K'),
    ('76c49cf5e9815cb799f96dd36c96180a',
     'MẸ MÌN', 'T18'),
    ('5747334836c1543f90d17b0669ec6ad6',
     'CLAYFACE', 'T18'),
    ('c8da77abf20d52adafada962622b3670',
     'HUYẾT THỐNG', 'T18'),
    ('bc9fa953fa1b57bcbd04921dc13c8133',
     'SỢI CHỈ ĐỎ', 'T18'),
    ('7fd6c616136551a0a94411668d638235',
     'AVENGERS: NGÀY TẬN THẾ', 'T13'),
    ('bcc1ac66cf36523f9282a0679b99f3c9',
     'DUNE: HÀNH TINH CÁT - PHẦN BA', 'T13'),
    ('aeb720fe135b5aaea0bb3b23453e8d50',
     'ÁN MẠNG KARAOKE (T16)', 'T16');

START TRANSACTION;

SET @cinestar_age_rating_now = UTC_TIMESTAMP(6);

UPDATE movies AS m
JOIN cinestar_age_rating_seed AS s
    ON m.id = UNHEX(s.movie_id_hex)
   AND CAST(m.title AS BINARY) = CAST(s.title AS BINARY)
SET
    m.age_rating = s.age_rating,
    m.version = m.version + 1,
    m.updated_at = @cinestar_age_rating_now
WHERE m.age_rating IS NULL;

SELECT ROW_COUNT() AS updated_movies;

-- Expected: 40 matched rows when the original seed is intact.
SELECT
    COUNT(*) AS classified_source_movies,
    COUNT(m.id) AS matched_movies
FROM cinestar_age_rating_seed AS s
LEFT JOIN movies AS m
    ON m.id = UNHEX(s.movie_id_hex)
   AND CAST(m.title AS BINARY) = CAST(s.title AS BINARY);

-- Missing or renamed rows are reported, never inserted or guessed.
SELECT
    s.movie_id_hex,
    s.title AS expected_title
FROM cinestar_age_rating_seed AS s
LEFT JOIN movies AS m
    ON m.id = UNHEX(s.movie_id_hex)
   AND CAST(m.title AS BINARY) = CAST(s.title AS BINARY)
WHERE m.id IS NULL;

-- Existing classifications are preserved, including disagreements.
SELECT
    m.title,
    m.age_rating AS preserved_rating,
    s.age_rating AS source_rating
FROM movies AS m
JOIN cinestar_age_rating_seed AS s
    ON m.id = UNHEX(s.movie_id_hex)
   AND CAST(m.title AS BINARY) = CAST(s.title AS BINARY)
WHERE m.age_rating <> s.age_rating;

COMMIT;

DROP TEMPORARY TABLE cinestar_age_rating_seed;
