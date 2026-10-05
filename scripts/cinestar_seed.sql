-- CINESTAR PUBLIC DATA -> CINEMA SYSTEM / MYSQL 8.x
-- Snapshot UTC: 2026-10-05T03:20:50.406854+00:00
-- Backend main commit: a3fd0fc0fe422e8b655ffdd47a11161b2bb850df
-- Schema: movie-service V1__create_movie_tables.sql; inventory-service V1__create_inventory_schema.sql.
-- Enum values verified in MovieStatus.java, RoomType.java and ShowtimeStatus.java.
-- Sources: https://cinestar.com.vn/ and the public movie pages listed below.
-- Chay sau khi Flyway da tao bang. Script KHONG tao database/bang va KHONG sua migration.
-- Neu DB_URL khac mac dinh: sua HAI TEN DATABASE trong TAT CA lenh USE truoc khi chay.
-- Chay TOAN BO FILE trong CUNG MOT connection; tai khoan SQL can quyen tren ca hai database.
-- Bat che do dung khi gap loi. Neu co loi: dung lai va ROLLBACK; khong chay COMMIT.
-- Khong chay dong thoi nhieu ban sao script.
-- Chi INSERT ban ghi/quan he con thieu; khong DELETE/TRUNCATE/UPDATE du lieu hien co.
-- UUID seed la UUIDv5 on dinh theo loai ban ghi + Cinestar source ID; BINARY(16) khong swap byte.
-- Movies/genres duoc doi chieu theo title/name; cinemas theo name + address; rooms theo cinema + name.
-- Giu nguyen cac ban LT/PĐ va hau to phan loai trong title; khong tao field age_rating.
-- NOW_SHOWING/UPCOMING theo danh sach nguon, khong tu suy ra tu release_date.
-- Mot so phim sap chieu co time=99 tren nguon. Giu gia tri nguon; khong coi la runtime da kiem chung.
-- description = NULL. Khong sao chep toan bo noi dung gioi thieu/marketing.
-- poster_url la URL anh cong khai; khong tai/copy file anh. Trailer ID -> URL YouTube HTTPS.
-- Chi map room_type_name_en=Standard -> STANDARD. Deluxe chua co mapping: bo qua.
-- starts_at: gio lich chieu Asia/Ho_Chi_Minh (+07:00) -> UTC DATETIME.
-- ends_at DUOC SUY RA = starts_at + duration_minutes; nguon khong cung cap gio ket thuc.
-- Khong them thoi gian quang cao/don phong va khong dich lich sang ngay hien tai.
-- Tat ca showtimes seed = SCHEDULED. KHONG tu mo ban: chua co seats/show_seats/gia ve tin cay.
-- Endpoint /showtimes/bookable se KHONG tra ve cac suat SCHEDULED nay.
-- Bo qua suat da bat dau khi lay snapshot; khi chay SQL tiep tuc bo qua suat da bat dau.
-- Bo qua suat trung/overlap voi showtimes hien co trong phong (tru CANCELLED).
-- Khong seed account/booking/hold/payment, promotion, membership, age_rating hay backdrop.
-- So luong toi da tren DB rong, chay ngay sau snapshot: {"baseline": "a3fd0fc0fe422e8b655ffdd47a11161b2bb850df", "snapshot_utc": "2026-10-05T03:20:50.406854+00:00", "movies": 54, "genres": 17, "movie_genres": 86, "cinemas": 10, "rooms": 48, "showtimes": 650, "poster_urls": 54, "trailer_urls": 31, "status_counts": {"NOW_SHOWING": 31, "UPCOMING": 23}, "excluded_events": {"unsupported_room_type": 55, "started_before_snapshot": 11, "overlapping_source_event": 0}, "showtime_first_utc": "2026-10-05T03:25:00+00:00", "showtime_last_utc": "2026-10-17T13:30:00+00:00"}
-- Lich chieu co the thay doi sau snapshot. Day la du lieu seed phat trien, khong dong bo live.
-- Public schedule sources:
-- https://cinestar.com.vn/movie/18aa3129-b784-4f31-884a-86bce3cdeb04/
-- https://cinestar.com.vn/movie/3d564340-eb10-4e0b-a6fd-adfaeefdf9a3/
-- https://cinestar.com.vn/movie/41426706-8d0a-4468-8ced-2099b57222da/
-- https://cinestar.com.vn/movie/47f644e0-dd2a-4668-a129-a3e9d09d1bfb/
-- https://cinestar.com.vn/movie/4fdd2185-0653-4413-889a-95f1e0dc8f00/
-- https://cinestar.com.vn/movie/52677cea-e24c-4dc7-af48-8e3972cbb28a/
-- https://cinestar.com.vn/movie/628aef99-3632-4449-8968-e59ec91d43a5/
-- https://cinestar.com.vn/movie/68db431d-b3e7-4f80-a36f-7bcefb1ee4a3/
-- https://cinestar.com.vn/movie/82c40625-cdf6-4692-8662-fdb1755f9126/
-- https://cinestar.com.vn/movie/8907c33a-0f25-48cb-ba66-caca866ab99a/
-- https://cinestar.com.vn/movie/b07925ab-2c6e-489f-903d-8a376b743c12/
-- https://cinestar.com.vn/movie/c4b7e4bb-3f7d-4151-9d40-caab9e6a5c81/
-- https://cinestar.com.vn/movie/d3742d45-e804-4143-9ca1-e6793889ed7c/
-- https://cinestar.com.vn/movie/e608c4c5-e2a5-4934-b80d-af0c89ff8497/

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @cinestar_seed_previous_time_zone = @@SESSION.time_zone;
SET SESSION time_zone = '+00:00';
SET @cinestar_seed_now = UTC_TIMESTAMP(6);
START TRANSACTION;

USE cinema_movie_db;

-- 1. GENRES
-- Genre: Anime
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('e5360dd1c6245de78d915caa7fafca2a') OR name = 'Anime');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('e5360dd1c6245de78d915caa7fafca2a'), 'Anime', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_01 = (SELECT id FROM genres WHERE name = 'Anime' LIMIT 1);

-- Genre: Chính Kịch
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('5c436f4f054959008cdaaecfa407198b') OR name = 'Chính Kịch');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('5c436f4f054959008cdaaecfa407198b'), 'Chính Kịch', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_02 = (SELECT id FROM genres WHERE name = 'Chính Kịch' LIMIT 1);

-- Genre: Drama
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('9ff1ace737945343b1f626ce376dc752') OR name = 'Drama');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('9ff1ace737945343b1f626ce376dc752'), 'Drama', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_03 = (SELECT id FROM genres WHERE name = 'Drama' LIMIT 1);

-- Genre: Gia đình
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('5351b43a749e5873bbf26c1b92a1cf4e') OR name = 'Gia đình');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('5351b43a749e5873bbf26c1b92a1cf4e'), 'Gia đình', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_04 = (SELECT id FROM genres WHERE name = 'Gia đình' LIMIT 1);

-- Genre: Hoạt hình
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('317fa0078280588386e5c444bd886fc0') OR name = 'Hoạt hình');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('317fa0078280588386e5c444bd886fc0'), 'Hoạt hình', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_05 = (SELECT id FROM genres WHERE name = 'Hoạt hình' LIMIT 1);

-- Genre: Hài
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('2a3c646e8aff540bae6fa9d16caf0164') OR name = 'Hài');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('2a3c646e8aff540bae6fa9d16caf0164'), 'Hài', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_06 = (SELECT id FROM genres WHERE name = 'Hài' LIMIT 1);

-- Genre: Hành Động
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('7c2ea98bea815626a3b2e0617b897a8c') OR name = 'Hành Động');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('7c2ea98bea815626a3b2e0617b897a8c'), 'Hành Động', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_07 = (SELECT id FROM genres WHERE name = 'Hành Động' LIMIT 1);

-- Genre: Hồi Hộp
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('1bac7082157458b6a6b11b596a0f42e8') OR name = 'Hồi Hộp');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('1bac7082157458b6a6b11b596a0f42e8'), 'Hồi Hộp', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_08 = (SELECT id FROM genres WHERE name = 'Hồi Hộp' LIMIT 1);

-- Genre: Khoa Học Viễn Tưởng
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('d8b8b42a67ea53ea9d973df027760c3d') OR name = 'Khoa Học Viễn Tưởng');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('d8b8b42a67ea53ea9d973df027760c3d'), 'Khoa Học Viễn Tưởng', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_09 = (SELECT id FROM genres WHERE name = 'Khoa Học Viễn Tưởng' LIMIT 1);

-- Genre: Kinh Dị
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('621674882e5550c3915f7c2d248276d1') OR name = 'Kinh Dị');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('621674882e5550c3915f7c2d248276d1'), 'Kinh Dị', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_10 = (SELECT id FROM genres WHERE name = 'Kinh Dị' LIMIT 1);

-- Genre: Phim Ca Nhạc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('eb5cf4b5664d5b65b006a05ff295f719') OR name = 'Phim Ca Nhạc');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('eb5cf4b5664d5b65b006a05ff295f719'), 'Phim Ca Nhạc', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_11 = (SELECT id FROM genres WHERE name = 'Phim Ca Nhạc' LIMIT 1);

-- Genre: Phiêu Lưu
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('fb5712e284265cc8b53e2e8d00ad8c08') OR name = 'Phiêu Lưu');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('fb5712e284265cc8b53e2e8d00ad8c08'), 'Phiêu Lưu', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_12 = (SELECT id FROM genres WHERE name = 'Phiêu Lưu' LIMIT 1);

-- Genre: Siêu Anh Hùng
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('248b0403dc595960a4deaa5396836f4b') OR name = 'Siêu Anh Hùng');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('248b0403dc595960a4deaa5396836f4b'), 'Siêu Anh Hùng', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_13 = (SELECT id FROM genres WHERE name = 'Siêu Anh Hùng' LIMIT 1);

-- Genre: Sử Thi
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('614739493fde5cb3ae63e31b10843f30') OR name = 'Sử Thi');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('614739493fde5cb3ae63e31b10843f30'), 'Sử Thi', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_14 = (SELECT id FROM genres WHERE name = 'Sử Thi' LIMIT 1);

-- Genre: Thần Thoại
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('92059f1b376052de86d6ecd495de60c6') OR name = 'Thần Thoại');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('92059f1b376052de86d6ecd495de60c6'), 'Thần Thoại', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_15 = (SELECT id FROM genres WHERE name = 'Thần Thoại' LIMIT 1);

-- Genre: Tâm Lý
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('cd2489c832f958379826f85590f29e36') OR name = 'Tâm Lý');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('cd2489c832f958379826f85590f29e36'), 'Tâm Lý', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_16 = (SELECT id FROM genres WHERE name = 'Tâm Lý' LIMIT 1);

-- Genre: Tình Cảm
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM genres WHERE id = UNHEX('2148443aec6f5077a42997120dc2e357') OR name = 'Tình Cảm');
INSERT INTO genres (id, name, description, version, created_at, updated_at)
SELECT UNHEX('2148443aec6f5077a42997120dc2e357'), 'Tình Cảm', NULL, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_genre_17 = (SELECT id FROM genres WHERE name = 'Tình Cảm' LIMIT 1);

-- 2. MOVIES + MOVIE_GENRES
-- Source: https://cinestar.com.vn/movie/68db431d-b3e7-4f80-a36f-7bcefb1ee4a3/
-- Movie: HÒN ĐẢO QUÊN LÃNG LT (K)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('03d1cd6dbc32579cb4ac2556c09d87b5') OR title = 'HÒN ĐẢO QUÊN LÃNG LT (K)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('03d1cd6dbc32579cb4ac2556c09d87b5'), 'HÒN ĐẢO QUÊN LÃNG LT (K)', NULL, 109, '2026-09-24', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/hon-dao-quen-lang_1.jpg', 'https://www.youtube.com/watch?v=6zAqU_dHK1M', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_01 = (SELECT id FROM movies WHERE title = 'HÒN ĐẢO QUÊN LÃNG LT (K)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_01 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_01, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_01 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_01 AND genre_id = @cinestar_seed_genre_12);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_01, @cinestar_seed_genre_12 FROM DUAL
WHERE @cinestar_seed_movie_01 IS NOT NULL AND @cinestar_seed_genre_12 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/41426706-8d0a-4468-8ced-2099b57222da/
-- Movie: PHÁO HOA LÚC BÌNH MINH (P)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('a7ba608e7331585cb577a2fccfca21a9') OR title = 'PHÁO HOA LÚC BÌNH MINH (P)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('a7ba608e7331585cb577a2fccfca21a9'), 'PHÁO HOA LÚC BÌNH MINH (P)', NULL, 76, '2026-09-25', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/PHAO-HOA-LUC-BINH-MINH.jpg', 'https://www.youtube.com/watch?v=IiIydHpK0PQ', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_02 = (SELECT id FROM movies WHERE title = 'PHÁO HOA LÚC BÌNH MINH (P)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_02 AND genre_id = @cinestar_seed_genre_01);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_02, @cinestar_seed_genre_01 FROM DUAL
WHERE @cinestar_seed_movie_02 IS NOT NULL AND @cinestar_seed_genre_01 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/2d3d37d4-32f1-4e9b-8bad-551ecb6545d1/
-- Movie: HÒN ĐẢO QUÊN LÃNG PĐ (K)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('61e9d70d165b5f9fa5615877b6d4bb30') OR title = 'HÒN ĐẢO QUÊN LÃNG PĐ (K)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('61e9d70d165b5f9fa5615877b6d4bb30'), 'HÒN ĐẢO QUÊN LÃNG PĐ (K)', NULL, 109, '2026-09-25', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/hon-dao-quen-lang_1.jpg', 'https://www.youtube.com/watch?v=6zAqU_dHK1M', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_03 = (SELECT id FROM movies WHERE title = 'HÒN ĐẢO QUÊN LÃNG PĐ (K)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_03 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_03, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_03 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_03 AND genre_id = @cinestar_seed_genre_12);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_03, @cinestar_seed_genre_12 FROM DUAL
WHERE @cinestar_seed_movie_03 IS NOT NULL AND @cinestar_seed_genre_12 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/8907c33a-0f25-48cb-ba66-caca866ab99a/
-- Movie: QUYẾT CUA ANH NÀY! (PĐ) (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('51ff33807d57537398d4015711f374f9') OR title = 'QUYẾT CUA ANH NÀY! (PĐ) (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('51ff33807d57537398d4015711f374f9'), 'QUYẾT CUA ANH NÀY! (PĐ) (T13)', NULL, 115, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/henry.jpg', 'https://www.youtube.com/watch?v=ngOAzhgp5y4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_04 = (SELECT id FROM movies WHERE title = 'QUYẾT CUA ANH NÀY! (PĐ) (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_04 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_04, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_04 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_04 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_04, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_04 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/47f644e0-dd2a-4668-a129-a3e9d09d1bfb/
-- Movie: KHÓA CHẶT CỬA NÀO SUZUME (P)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('059fc591d31f58b7a5fa86e9d8ad4176') OR title = 'KHÓA CHẶT CỬA NÀO SUZUME (P)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('059fc591d31f58b7a5fa86e9d8ad4176'), 'KHÓA CHẶT CỬA NÀO SUZUME (P)', NULL, 122, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/SUZUME.jpg', 'https://www.youtube.com/watch?v=v1nOJFrOnfU', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_05 = (SELECT id FROM movies WHERE title = 'KHÓA CHẶT CỬA NÀO SUZUME (P)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_05 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_05, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_05 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_05 AND genre_id = @cinestar_seed_genre_12);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_05, @cinestar_seed_genre_12 FROM DUAL
WHERE @cinestar_seed_movie_05 IS NOT NULL AND @cinestar_seed_genre_12 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/3d564340-eb10-4e0b-a6fd-adfaeefdf9a3/
-- Movie: THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('9393ae2d77a1532a8dacaa742b64c60d') OR title = 'THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('9393ae2d77a1532a8dacaa742b64c60d'), 'THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13)', NULL, 96, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/master-zhong.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_06 = (SELECT id FROM movies WHERE title = 'THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_06 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_06, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_06 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/c4b7e4bb-3f7d-4151-9d40-caab9e6a5c81/
-- Movie: QUYẾT CUA ANH NÀY! LT (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('4b1a74f4829050dd96cc21890b67e1cc') OR title = 'QUYẾT CUA ANH NÀY! LT (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('4b1a74f4829050dd96cc21890b67e1cc'), 'QUYẾT CUA ANH NÀY! LT (T13)', NULL, 115, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/henry.jpg', 'https://www.youtube.com/watch?v=ngOAzhgp5y4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_07 = (SELECT id FROM movies WHERE title = 'QUYẾT CUA ANH NÀY! LT (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_07 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_07, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_07 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_07 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_07, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_07 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/82c40625-cdf6-4692-8662-fdb1755f9126/
-- Movie: SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('881b3c0bfec4545fb496a0f296db6ed1') OR title = 'SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('881b3c0bfec4545fb496a0f296db6ed1'), 'SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P)', NULL, 80, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/scotty.jpg', 'https://www.youtube.com/watch?v=kLSxi-X5Z04', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_08 = (SELECT id FROM movies WHERE title = 'SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_08 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_08, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_08 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_08 AND genre_id = @cinestar_seed_genre_12);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_08, @cinestar_seed_genre_12 FROM DUAL
WHERE @cinestar_seed_movie_08 IS NOT NULL AND @cinestar_seed_genre_12 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_08 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_08, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_08 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/61dce0d6-b2a3-40d4-b858-08a18281271d/
-- Movie: QUỶ ĂN TẠNG 4: HỔ TINH LT (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('a9d69ba3591b5d8ba4b512aa718bace2') OR title = 'QUỶ ĂN TẠNG 4: HỔ TINH LT (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('a9d69ba3591b5d8ba4b512aa718bace2'), 'QUỶ ĂN TẠNG 4: HỔ TINH LT (T18)', NULL, 111, '2026-10-09', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/ty4.jpg', 'https://www.youtube.com/watch?v=c2g0FM50F2E', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_09 = (SELECT id FROM movies WHERE title = 'QUỶ ĂN TẠNG 4: HỔ TINH LT (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_09 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_09, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_09 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/628aef99-3632-4449-8968-e59ec91d43a5/
-- Movie: ÁN MẠNG XÉM HOÀN HẢO (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('b03297fc2c9653f4906ecd30ae373087') OR title = 'ÁN MẠNG XÉM HOÀN HẢO (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('b03297fc2c9653f4906ecd30ae373087'), 'ÁN MẠNG XÉM HOÀN HẢO (T18)', NULL, 124, '2026-10-09', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/an-mang_1.jpg', 'https://www.youtube.com/watch?v=O5pbknSRxps', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_10 = (SELECT id FROM movies WHERE title = 'ÁN MẠNG XÉM HOÀN HẢO (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_10 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_10, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_10 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/251225fe-32cb-4838-b88c-312eb9fe755f/
-- Movie: QUỶ ĂN TẠNG 4: HỔ TINH PĐ (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('6b802086780a5a6fa1ba590683709c33') OR title = 'QUỶ ĂN TẠNG 4: HỔ TINH PĐ (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('6b802086780a5a6fa1ba590683709c33'), 'QUỶ ĂN TẠNG 4: HỔ TINH PĐ (T18)', NULL, 111, '2026-10-09', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/ty4.jpg', 'https://www.youtube.com/watch?v=c2g0FM50F2E', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_11 = (SELECT id FROM movies WHERE title = 'QUỶ ĂN TẠNG 4: HỔ TINH PĐ (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_11 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_11, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_11 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_11 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_11, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_11 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/52677cea-e24c-4dc7-af48-8e3972cbb28a/
-- Movie: ALWAYS LALISA
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('3aad75ab42e65dd581d3bc5d67a2d7f9') OR title = 'ALWAYS LALISA');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('3aad75ab42e65dd581d3bc5d67a2d7f9'), 'ALWAYS LALISA', NULL, 98, '2026-10-12', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/lalisa.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_12 = (SELECT id FROM movies WHERE title = 'ALWAYS LALISA' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_12 AND genre_id = @cinestar_seed_genre_11);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_12, @cinestar_seed_genre_11 FROM DUAL
WHERE @cinestar_seed_movie_12 IS NOT NULL AND @cinestar_seed_genre_11 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/d3742d45-e804-4143-9ca1-e6793889ed7c/
-- Movie: ÚT LAN 2 (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('3d3a1a25fc4256328a12b5cae9dde455') OR title = 'ÚT LAN 2 (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('3d3a1a25fc4256328a12b5cae9dde455'), 'ÚT LAN 2 (T18)', NULL, 107, '2026-09-21', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/ut-lan.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_13 = (SELECT id FROM movies WHERE title = 'ÚT LAN 2 (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_13 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_13, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_13 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/4fdd2185-0653-4413-889a-95f1e0dc8f00/
-- Movie: LÊN HƯƠNG (T16)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('daf28a3d701c54499ae7fe3405eb5f55') OR title = 'LÊN HƯƠNG (T16)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('daf28a3d701c54499ae7fe3405eb5f55'), 'LÊN HƯƠNG (T16)', NULL, 121, '2026-09-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/len-huong-poster.jpg', 'https://www.youtube.com/watch?v=uVwMeluLLzk', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_14 = (SELECT id FROM movies WHERE title = 'LÊN HƯƠNG (T16)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_14 AND genre_id = @cinestar_seed_genre_04);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_14, @cinestar_seed_genre_04 FROM DUAL
WHERE @cinestar_seed_movie_14 IS NOT NULL AND @cinestar_seed_genre_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_14 AND genre_id = @cinestar_seed_genre_16);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_14, @cinestar_seed_genre_16 FROM DUAL
WHERE @cinestar_seed_movie_14 IS NOT NULL AND @cinestar_seed_genre_16 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/b07925ab-2c6e-489f-903d-8a376b743c12/
-- Movie: TRẠI BUÔN NGƯỜI (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('43e0179c3af554cd93f65a9099626076') OR title = 'TRẠI BUÔN NGƯỜI (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('43e0179c3af554cd93f65a9099626076'), 'TRẠI BUÔN NGƯỜI (T18)', NULL, 135, '2026-09-25', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/trai-buon-nguoi-poster.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_15 = (SELECT id FROM movies WHERE title = 'TRẠI BUÔN NGƯỜI (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_15 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_15, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_15 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/bde1b23c-c243-4b8a-92b0-136bf288a532/
-- Movie: BÓNG MA NHÀ HÁT (T16)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('ae0c431510f8513eb3473c536ac70b4f') OR title = 'BÓNG MA NHÀ HÁT (T16)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('ae0c431510f8513eb3473c536ac70b4f'), 'BÓNG MA NHÀ HÁT (T16)', NULL, 97, '2026-09-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/bong-ma.jpg', 'https://www.youtube.com/watch?v=zMcfp2DpMn8', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_16 = (SELECT id FROM movies WHERE title = 'BÓNG MA NHÀ HÁT (T16)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_16 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_16, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_16 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_16 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_16, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_16 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/778f8171-20aa-4ea7-add6-43bf94392f27/
-- Movie: VÙNG ĐẤT QUỶ DỮ (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('771713ed20035beab0068ee6bfbf4adf') OR title = 'VÙNG ĐẤT QUỶ DỮ (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('771713ed20035beab0068ee6bfbf4adf'), 'VÙNG ĐẤT QUỶ DỮ (T18)', NULL, 94, '2026-09-11', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/vung-dat-quy-du.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_17 = (SELECT id FROM movies WHERE title = 'VÙNG ĐẤT QUỶ DỮ (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_17 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_17, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_17 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/fe19c512-3fd3-4aa6-870c-10912bb8c0b0/
-- Movie: HỘ LINH TRÁNG SĨ (T13): BÍ ẨN MỘ VUA ĐINH (TG)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('f558db23f7635adf8c8d2b722152d8ed') OR title = 'HỘ LINH TRÁNG SĨ (T13): BÍ ẨN MỘ VUA ĐINH (TG)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('f558db23f7635adf8c8d2b722152d8ed'), 'HỘ LINH TRÁNG SĨ (T13): BÍ ẨN MỘ VUA ĐINH (TG)', NULL, 135, '2026-08-28', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/08-2026/ho-linh-trang-si-tg.jpg', 'https://www.youtube.com/watch?v=mYuWqh8nSC4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_18 = (SELECT id FROM movies WHERE title = 'HỘ LINH TRÁNG SĨ (T13): BÍ ẨN MỘ VUA ĐINH (TG)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_18 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_18, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_18 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/b080b97d-c762-485c-89a1-ea6251dfa1e2/
-- Movie: HỘ LINH TRÁNG SĨ: BÍ ẨN MỘ VUA ĐINH (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('ea78d048566f54df8d87ea54c8b95d31') OR title = 'HỘ LINH TRÁNG SĨ: BÍ ẨN MỘ VUA ĐINH (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('ea78d048566f54df8d87ea54c8b95d31'), 'HỘ LINH TRÁNG SĨ: BÍ ẨN MỘ VUA ĐINH (T13)', NULL, 155, '2026-08-28', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/ho-linh-trang-si-poster.jpg', 'https://www.youtube.com/watch?v=mYuWqh8nSC4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_19 = (SELECT id FROM movies WHERE title = 'HỘ LINH TRÁNG SĨ: BÍ ẨN MỘ VUA ĐINH (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_19 AND genre_id = @cinestar_seed_genre_14);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_19, @cinestar_seed_genre_14 FROM DUAL
WHERE @cinestar_seed_movie_19 IS NOT NULL AND @cinestar_seed_genre_14 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_19 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_19, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_19 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/e608c4c5-e2a5-4934-b80d-af0c89ff8497/
-- Movie: AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('3bede48f3be35294ad1b070300517078') OR title = 'AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('3bede48f3be35294ad1b070300517078'), 'AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13)', NULL, 183, '2026-09-25', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/end-game.jpg', 'https://www.youtube.com/watch?v=v5us1p8srD4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_20 = (SELECT id FROM movies WHERE title = 'AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_20 AND genre_id = @cinestar_seed_genre_13);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_20, @cinestar_seed_genre_13 FROM DUAL
WHERE @cinestar_seed_movie_20 IS NOT NULL AND @cinestar_seed_genre_13 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_20 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_20, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_20 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_20 AND genre_id = @cinestar_seed_genre_09);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_20, @cinestar_seed_genre_09 FROM DUAL
WHERE @cinestar_seed_movie_20 IS NOT NULL AND @cinestar_seed_genre_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/2950f27b-77c3-4c66-9637-dc2c21c2d3fe/
-- Movie: TÀU BUÔN NGƯỜI (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('c872f64f835e5697b1f92d2b2574cc67') OR title = 'TÀU BUÔN NGƯỜI (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('c872f64f835e5697b1f92d2b2574cc67'), 'TÀU BUÔN NGƯỜI (T18)', NULL, 95, '2026-08-28', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/08-2026/tau-buon-nguoi-poster.jpg', 'https://www.youtube.com/watch?v=CH7AgfKEhW4', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_21 = (SELECT id FROM movies WHERE title = 'TÀU BUÔN NGƯỜI (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_21 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_21, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_21 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/fee0eeea-58db-4f57-8bdf-36428fa9a929/
-- Movie: CỔ THUẬT HẮC NGẢI (T16)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('1f59b15c13de5ae4a0f4b0a5d53793db') OR title = 'CỔ THUẬT HẮC NGẢI (T16)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('1f59b15c13de5ae4a0f4b0a5d53793db'), 'CỔ THUẬT HẮC NGẢI (T16)', NULL, 86, '2026-09-11', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/co-thuat-hac-ngai.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_22 = (SELECT id FROM movies WHERE title = 'CỔ THUẬT HẮC NGẢI (T16)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_22 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_22, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_22 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/3c9fc307-8769-40f0-8ae4-489d4d1ee233/
-- Movie: BÙA YÊU: BÍ MẬT GIA TỘC (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('6ee15a2d45185a21a463d420e89a0416') OR title = 'BÙA YÊU: BÍ MẬT GIA TỘC (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('6ee15a2d45185a21a463d420e89a0416'), 'BÙA YÊU: BÍ MẬT GIA TỘC (T13)', NULL, 130, '2026-09-11', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/bua-yeu.jpg', 'https://www.youtube.com/watch?v=h5ikz9V8BJ8', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_23 = (SELECT id FROM movies WHERE title = 'BÙA YÊU: BÍ MẬT GIA TỘC (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_23 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_23, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_23 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_23 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_23, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_23 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/c56bb75c-793d-4316-be58-50dd550dd778/
-- Movie: MA TÙ LT (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('fde155d92ef450caac5187c1c9e54b38') OR title = 'MA TÙ LT (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('fde155d92ef450caac5187c1c9e54b38'), 'MA TÙ LT (T18)', NULL, 105, '2026-09-04', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/ma-tu.jpg', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_24 = (SELECT id FROM movies WHERE title = 'MA TÙ LT (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_24 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_24, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_24 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/1313b952-017c-445a-8388-9c7d2b7da839/
-- Movie: NGHỈ HÈ SỢ NGHỈ HƯU (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('f5ddf8feebff553ba6d37866dad9e9a2') OR title = 'NGHỈ HÈ SỢ NGHỈ HƯU (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('f5ddf8feebff553ba6d37866dad9e9a2'), 'NGHỈ HÈ SỢ NGHỈ HƯU (T13)', NULL, 117, '2026-08-21', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/08-2026/nhsnh_1.jpg', 'https://www.youtube.com/watch?v=BtnVSMZJgDg', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_25 = (SELECT id FROM movies WHERE title = 'NGHỈ HÈ SỢ NGHỈ HƯU (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_25 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_25, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_25 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_25 AND genre_id = @cinestar_seed_genre_04);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_25, @cinestar_seed_genre_04 FROM DUAL
WHERE @cinestar_seed_movie_25 IS NOT NULL AND @cinestar_seed_genre_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/a9ac00a3-353d-4bce-80b9-bb5e33130778/
-- Movie: HOPE VÙNG TỬ ĐỊA (T16)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('e91d795a12865e3a81ab54fa3f7041fe') OR title = 'HOPE VÙNG TỬ ĐỊA (T16)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('e91d795a12865e3a81ab54fa3f7041fe'), 'HOPE VÙNG TỬ ĐỊA (T16)', NULL, 99, '2026-09-04', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/hope.jpg', 'https://www.youtube.com/watch?v=aLdm8oL6PYg', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_26 = (SELECT id FROM movies WHERE title = 'HOPE VÙNG TỬ ĐỊA (T16)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_26 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_26, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_26 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_26 AND genre_id = @cinestar_seed_genre_09);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_26, @cinestar_seed_genre_09 FROM DUAL
WHERE @cinestar_seed_movie_26 IS NOT NULL AND @cinestar_seed_genre_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/11365d7c-0ddd-4051-9791-11bc43e5edfa/
-- Movie: YÊU NHÂN THẦN THÁM: KỲ ÁN TRƯỜNG AN (K)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('b7b63d5a787e51bd9a8b3d51587a7547') OR title = 'YÊU NHÂN THẦN THÁM: KỲ ÁN TRƯỜNG AN (K)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('b7b63d5a787e51bd9a8b3d51587a7547'), 'YÊU NHÂN THẦN THÁM: KỲ ÁN TRƯỜNG AN (K)', NULL, 116, '2026-09-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/yeu-nhan.jpg', 'https://www.youtube.com/watch?v=aOyuxI1JMKE', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_27 = (SELECT id FROM movies WHERE title = 'YÊU NHÂN THẦN THÁM: KỲ ÁN TRƯỜNG AN (K)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_27 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_27, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_27 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/76753206-2493-45a8-b21d-41c578bb6e2e/
-- Movie: BÁT TIÊN ! TRUY TÌM LƯU LY ĐĂNG (K)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('cf8d00c7ff8b5d16a7a2e134d8db2af4') OR title = 'BÁT TIÊN ! TRUY TÌM LƯU LY ĐĂNG (K)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('cf8d00c7ff8b5d16a7a2e134d8db2af4'), 'BÁT TIÊN ! TRUY TÌM LƯU LY ĐĂNG (K)', NULL, 144, '2026-09-11', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/bat-tien.jpg', 'https://www.youtube.com/watch?v=DE3xVN6RY4k', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_28 = (SELECT id FROM movies WHERE title = 'BÁT TIÊN ! TRUY TÌM LƯU LY ĐĂNG (K)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_28 AND genre_id = @cinestar_seed_genre_15);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_28, @cinestar_seed_genre_15 FROM DUAL
WHERE @cinestar_seed_movie_28 IS NOT NULL AND @cinestar_seed_genre_15 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_28 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_28, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_28 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/18aa3129-b784-4f31-884a-86bce3cdeb04/
-- Movie: TRÁI TIM QUÁI THÚ (T13)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('7dd7d64de5c95612814ff7204358d4ab') OR title = 'TRÁI TIM QUÁI THÚ (T13)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('7dd7d64de5c95612814ff7204358d4ab'), 'TRÁI TIM QUÁI THÚ (T13)', NULL, 101, '2026-10-02', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/trai-tim-quai-thu.png', NULL, 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_29 = (SELECT id FROM movies WHERE title = 'TRÁI TIM QUÁI THÚ (T13)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_29 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_29, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_29 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/5451a720-c7c3-4d17-ae4d-c4c7e17fb608/
-- Movie: PHIM SHIN – CẬU BÉ BÚT CHÌ: KỲ KỲ QUÁI QUÁI! KỲ NGHỈ YÊU QUÁI CỦA TỚ LT (P)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('58b56da983fe5a1ea78973e062842632') OR title = 'PHIM SHIN – CẬU BÉ BÚT CHÌ: KỲ KỲ QUÁI QUÁI! KỲ NGHỈ YÊU QUÁI CỦA TỚ LT (P)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('58b56da983fe5a1ea78973e062842632'), 'PHIM SHIN – CẬU BÉ BÚT CHÌ: KỲ KỲ QUÁI QUÁI! KỲ NGHỈ YÊU QUÁI CỦA TỚ LT (P)', NULL, 101, '2026-08-21', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/08-2026/shinn.jpg', 'https://www.youtube.com/watch?v=GR_kHLWYASg', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_30 = (SELECT id FROM movies WHERE title = 'PHIM SHIN – CẬU BÉ BÚT CHÌ: KỲ KỲ QUÁI QUÁI! KỲ NGHỈ YÊU QUÁI CỦA TỚ LT (P)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_30 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_30, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_30 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/5b894457-ea2d-4348-a4b0-0ff0f79d8ba6/
-- Movie: TẾ NHI CẢI MỆNH (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('d4866186b88951438d7cdf5c6951fbc2') OR title = 'TẾ NHI CẢI MỆNH (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('d4866186b88951438d7cdf5c6951fbc2'), 'TẾ NHI CẢI MỆNH (T18)', NULL, 104, '2026-09-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/09-2026/te-nhi-cai-menh.jpg', 'https://www.youtube.com/watch?v=w6uagGw-KJo', 'NOW_SHOWING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_31 = (SELECT id FROM movies WHERE title = 'TẾ NHI CẢI MỆNH (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_31 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_31, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_31 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/046b025c-b055-4487-b903-c955ff334f21/
-- Movie: PHÁN XÉT SỰ THẬT
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('3d5c41c4eae35717b9d0ec7e4a76ff15') OR title = 'PHÁN XÉT SỰ THẬT');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('3d5c41c4eae35717b9d0ec7e4a76ff15'), 'PHÁN XÉT SỰ THẬT', NULL, 99, '2026-10-09', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/the-social-reckoning.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_32 = (SELECT id FROM movies WHERE title = 'PHÁN XÉT SỰ THẬT' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_32 AND genre_id = @cinestar_seed_genre_02);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_32, @cinestar_seed_genre_02 FROM DUAL
WHERE @cinestar_seed_movie_32 IS NOT NULL AND @cinestar_seed_genre_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/5417a78e-5edc-4d30-84a2-238f73590e4f/
-- Movie: NGƯỜI MẸ KHÁC (T18)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('acd8170f4e4452b181d8054f70ea668b') OR title = 'NGƯỜI MẸ KHÁC (T18)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('acd8170f4e4452b181d8054f70ea668b'), 'NGƯỜI MẸ KHÁC (T18)', NULL, 93, '2026-10-09', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/other-mommy.jpg', 'https://www.youtube.com/watch?v=i5yivn8oDkk', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_33 = (SELECT id FROM movies WHERE title = 'NGƯỜI MẸ KHÁC (T18)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_33 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_33, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_33 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/077d9170-2c02-4a81-b277-1906d39ac1d2/
-- Movie: DIGGER
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('bca38d59eb795aa7992a854c9d76be79') OR title = 'DIGGER');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('bca38d59eb795aa7992a854c9d76be79'), 'DIGGER', NULL, 99, '2026-10-16', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/digger-1.jpg', 'https://www.youtube.com/watch?v=Dc1xkSO3Jq8', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_34 = (SELECT id FROM movies WHERE title = 'DIGGER' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_34 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_34, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_34 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_34 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_34, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_34 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/44c62959-d2ee-4b04-831c-2bb41b7d0ecb/
-- Movie: CHỊ CHỊ EM EM 3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('531539e54b245fcfbddac51da1778b9d') OR title = 'CHỊ CHỊ EM EM 3');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('531539e54b245fcfbddac51da1778b9d'), 'CHỊ CHỊ EM EM 3', NULL, 99, '2026-10-16', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/ccee3.jpg', 'https://www.youtube.com/watch?v=rwLELpYu57w', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_35 = (SELECT id FROM movies WHERE title = 'CHỊ CHỊ EM EM 3' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_35 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_35, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_35 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_35 AND genre_id = @cinestar_seed_genre_03);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_35, @cinestar_seed_genre_03 FROM DUAL
WHERE @cinestar_seed_movie_35 IS NOT NULL AND @cinestar_seed_genre_03 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_35 AND genre_id = @cinestar_seed_genre_16);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_35, @cinestar_seed_genre_16 FROM DUAL
WHERE @cinestar_seed_movie_35 IS NOT NULL AND @cinestar_seed_genre_16 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/fe6d22f0-3fce-4b6f-94fd-e9f416cc0e19/
-- Movie: STREET FIGHTER
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('66813ad790af52bea36b5bd5ed882a96') OR title = 'STREET FIGHTER');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('66813ad790af52bea36b5bd5ed882a96'), 'STREET FIGHTER', NULL, 99, '2026-10-16', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/street-fighter.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_36 = (SELECT id FROM movies WHERE title = 'STREET FIGHTER' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_36 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_36, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_36 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/c6276b68-28a1-4edf-8666-bad9ef6cad1e/
-- Movie: CÁ VOI NUỐT CHỬNG
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('77db1d9f6b8c5512b4d5f91c72b594c3') OR title = 'CÁ VOI NUỐT CHỬNG');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('77db1d9f6b8c5512b4d5f91c72b594c3'), 'CÁ VOI NUỐT CHỬNG', NULL, 99, '2026-10-16', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/whalefall.jpg', 'https://www.youtube.com/watch?v=EWuxqxi6_YY', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_37 = (SELECT id FROM movies WHERE title = 'CÁ VOI NUỐT CHỬNG' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_37 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_37, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_37 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/8b3ad10c-c377-4c6b-9834-23f420a3a1be/
-- Movie: CHUYỆN TÌNH KHAU VAI
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('5881129fb661566ea736e71451f481bf') OR title = 'CHUYỆN TÌNH KHAU VAI');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('5881129fb661566ea736e71451f481bf'), 'CHUYỆN TÌNH KHAU VAI', NULL, 116, '2026-10-16', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/chuyen-tinh-khau-vai.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_38 = (SELECT id FROM movies WHERE title = 'CHUYỆN TÌNH KHAU VAI' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_38 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_38, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_38 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_38 AND genre_id = @cinestar_seed_genre_16);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_38, @cinestar_seed_genre_16 FROM DUAL
WHERE @cinestar_seed_movie_38 IS NOT NULL AND @cinestar_seed_genre_16 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/53dc4345-efbb-420a-97f7-33c5121a96e3/
-- Movie: MẸ MÌN
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('76c49cf5e9815cb799f96dd36c96180a') OR title = 'MẸ MÌN');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('76c49cf5e9815cb799f96dd36c96180a'), 'MẸ MÌN', NULL, 99, '2026-10-23', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/mm.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_39 = (SELECT id FROM movies WHERE title = 'MẸ MÌN' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_39 AND genre_id = @cinestar_seed_genre_08);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_39, @cinestar_seed_genre_08 FROM DUAL
WHERE @cinestar_seed_movie_39 IS NOT NULL AND @cinestar_seed_genre_08 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/94de0c0d-3632-4f54-9595-656b7cc227f3/
-- Movie: CLAYFACE
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('5747334836c1543f90d17b0669ec6ad6') OR title = 'CLAYFACE');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('5747334836c1543f90d17b0669ec6ad6'), 'CLAYFACE', NULL, 99, '2026-10-23', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/clayface.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_40 = (SELECT id FROM movies WHERE title = 'CLAYFACE' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_40 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_40, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_40 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/a402aa0c-4fb8-4f7f-ae7d-62223e5fe394/
-- Movie: HUYẾT THỐNG
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('c8da77abf20d52adafada962622b3670') OR title = 'HUYẾT THỐNG');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('c8da77abf20d52adafada962622b3670'), 'HUYẾT THỐNG', NULL, 99, '2026-10-30', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/huyet-thong.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_41 = (SELECT id FROM movies WHERE title = 'HUYẾT THỐNG' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_41 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_41, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_41 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/715c4133-cf00-4f21-9dca-6af1fa054a99/
-- Movie: GODZILLA TRỪ KHÔNG
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('28646dce502d5461a9c6c65bd596b26a') OR title = 'GODZILLA TRỪ KHÔNG');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('28646dce502d5461a9c6c65bd596b26a'), 'GODZILLA TRỪ KHÔNG', NULL, 135, '2026-11-06', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/11-2026/godzilla.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_42 = (SELECT id FROM movies WHERE title = 'GODZILLA TRỪ KHÔNG' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_42 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_42, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_42 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_42 AND genre_id = @cinestar_seed_genre_09);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_42, @cinestar_seed_genre_09 FROM DUAL
WHERE @cinestar_seed_movie_42 IS NOT NULL AND @cinestar_seed_genre_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/5f4c980c-13ec-4a1c-a41f-4f150e5c6233/
-- Movie: CHÀNG MÈO MANG MŨ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('ea901781f5285c2ea74d07f9882a27fd') OR title = 'CHÀNG MÈO MANG MŨ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('ea901781f5285c2ea74d07f9882a27fd'), 'CHÀNG MÈO MANG MŨ', NULL, 99, '2026-11-06', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/11-2026/meo.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_43 = (SELECT id FROM movies WHERE title = 'CHÀNG MÈO MANG MŨ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_43 AND genre_id = @cinestar_seed_genre_05);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_43, @cinestar_seed_genre_05 FROM DUAL
WHERE @cinestar_seed_movie_43 IS NOT NULL AND @cinestar_seed_genre_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_43 AND genre_id = @cinestar_seed_genre_04);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_43, @cinestar_seed_genre_04 FROM DUAL
WHERE @cinestar_seed_movie_43 IS NOT NULL AND @cinestar_seed_genre_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_43 AND genre_id = @cinestar_seed_genre_12);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_43, @cinestar_seed_genre_12 FROM DUAL
WHERE @cinestar_seed_movie_43 IS NOT NULL AND @cinestar_seed_genre_12 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/e4e5897c-cf98-40ba-a767-cd350a06bef7/
-- Movie: BÒ SỮA BAY
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('e56ac4d686bf5ff1bf15798dc06d0c2f') OR title = 'BÒ SỮA BAY');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('e56ac4d686bf5ff1bf15798dc06d0c2f'), 'BÒ SỮA BAY', NULL, 99, '2026-11-06', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/11-2026/bsb.jpg', 'https://www.youtube.com/watch?v=QWamM7IZ5rU', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_44 = (SELECT id FROM movies WHERE title = 'BÒ SỮA BAY' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_44 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_44, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_44 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_44 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_44, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_44 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_44 AND genre_id = @cinestar_seed_genre_16);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_44, @cinestar_seed_genre_16 FROM DUAL
WHERE @cinestar_seed_movie_44 IS NOT NULL AND @cinestar_seed_genre_16 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/2db4caec-7d89-4518-bcb1-0441d749237e/
-- Movie: EBENEZER PHÉP MÀU ĐÊM GIÁNG SINH
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('e669fae6fa185c5fad78890cde3b4544') OR title = 'EBENEZER PHÉP MÀU ĐÊM GIÁNG SINH');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('e669fae6fa185c5fad78890cde3b4544'), 'EBENEZER PHÉP MÀU ĐÊM GIÁNG SINH', NULL, 99, '2026-11-13', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/11-2026/ebenezer.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_45 = (SELECT id FROM movies WHERE title = 'EBENEZER PHÉP MÀU ĐÊM GIÁNG SINH' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_45 AND genre_id = @cinestar_seed_genre_15);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_45, @cinestar_seed_genre_15 FROM DUAL
WHERE @cinestar_seed_movie_45 IS NOT NULL AND @cinestar_seed_genre_15 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/4f6f4386-7a24-4963-ab77-d41defca31c6/
-- Movie: NGÀY CON CÒN MẸ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('65e29ce4b6b45ed593c9bfef74ef81e9') OR title = 'NGÀY CON CÒN MẸ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('65e29ce4b6b45ed593c9bfef74ef81e9'), 'NGÀY CON CÒN MẸ', NULL, 130, '2026-11-13', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/ngay-con-con-me.jpg', 'https://www.youtube.com/watch?v=2fGs9SiBoiY', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_46 = (SELECT id FROM movies WHERE title = 'NGÀY CON CÒN MẸ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_46 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_46, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_46 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_46 AND genre_id = @cinestar_seed_genre_04);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_46, @cinestar_seed_genre_04 FROM DUAL
WHERE @cinestar_seed_movie_46 IS NOT NULL AND @cinestar_seed_genre_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/bbc79b65-730e-48ff-af5c-026859796f11/
-- Movie: GẶP GỠ THÔNG GIA: DÂU MỚI TRÌNH LÀNG
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('ca6ddcd8348357a89d865b58bda4c58a') OR title = 'GẶP GỠ THÔNG GIA: DÂU MỚI TRÌNH LÀNG');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('ca6ddcd8348357a89d865b58bda4c58a'), 'GẶP GỠ THÔNG GIA: DÂU MỚI TRÌNH LÀNG', NULL, 99, '2026-11-27', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/thong-gia.png', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_47 = (SELECT id FROM movies WHERE title = 'GẶP GỠ THÔNG GIA: DÂU MỚI TRÌNH LÀNG' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_47 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_47, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_47 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/a454ecc9-168f-4c37-a03b-efc971cd420b/
-- Movie: SỢI CHỈ ĐỎ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('bc9fa953fa1b57bcbd04921dc13c8133') OR title = 'SỢI CHỈ ĐỎ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('bc9fa953fa1b57bcbd04921dc13c8133'), 'SỢI CHỈ ĐỎ', NULL, 99, '2026-12-04', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/soi-chi-do.jpg', 'https://www.youtube.com/watch?v=ar9bh1Fzyaw', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_48 = (SELECT id FROM movies WHERE title = 'SỢI CHỈ ĐỎ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_48 AND genre_id = @cinestar_seed_genre_10);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_48, @cinestar_seed_genre_10 FROM DUAL
WHERE @cinestar_seed_movie_48 IS NOT NULL AND @cinestar_seed_genre_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/2d0ff8dc-7b2a-4b36-90f0-4c18bed87d99/
-- Movie: AVENGERS: NGÀY TẬN THẾ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('7fd6c616136551a0a94411668d638235') OR title = 'AVENGERS: NGÀY TẬN THẾ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('7fd6c616136551a0a94411668d638235'), 'AVENGERS: NGÀY TẬN THẾ', NULL, 165, '2026-12-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/12-2026/doomsday.jpg', 'https://www.youtube.com/watch?v=-bGVufP3uaQ', 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_49 = (SELECT id FROM movies WHERE title = 'AVENGERS: NGÀY TẬN THẾ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_49 AND genre_id = @cinestar_seed_genre_13);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_49, @cinestar_seed_genre_13 FROM DUAL
WHERE @cinestar_seed_movie_49 IS NOT NULL AND @cinestar_seed_genre_13 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_49 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_49, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_49 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_49 AND genre_id = @cinestar_seed_genre_09);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_49, @cinestar_seed_genre_09 FROM DUAL
WHERE @cinestar_seed_movie_49 IS NOT NULL AND @cinestar_seed_genre_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/bd01aace-eb90-4370-8296-e8d67269c86a/
-- Movie: DUNE: HÀNH TINH CÁT - PHẦN BA
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('bcc1ac66cf36523f9282a0679b99f3c9') OR title = 'DUNE: HÀNH TINH CÁT - PHẦN BA');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('bcc1ac66cf36523f9282a0679b99f3c9'), 'DUNE: HÀNH TINH CÁT - PHẦN BA', NULL, 99, '2026-12-18', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/12-2026/dune-p3.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_50 = (SELECT id FROM movies WHERE title = 'DUNE: HÀNH TINH CÁT - PHẦN BA' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_50 AND genre_id = @cinestar_seed_genre_15);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_50, @cinestar_seed_genre_15 FROM DUAL
WHERE @cinestar_seed_movie_50 IS NOT NULL AND @cinestar_seed_genre_15 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_50 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_50, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_50 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/a18c46f7-cc1a-48d7-af0b-f9228e0f8203/
-- Movie: NGƯỜI MẸ XẤU
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('424e4d1dfbeb5559b550a8f04fa3c6a9') OR title = 'NGƯỜI MẸ XẤU');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('424e4d1dfbeb5559b550a8f04fa3c6a9'), 'NGƯỜI MẸ XẤU', NULL, 99, '2026-12-31', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/12-2026/nguoi-me-xau-firstlook.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_51 = (SELECT id FROM movies WHERE title = 'NGƯỜI MẸ XẤU' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_51 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_51, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_51 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_51 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_51, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_51 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/f6ce5df5-be57-4b86-aba5-ac3bee9c8c0c/
-- Movie: LOẠN THẾ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('f9a22933b9bd527b8b9370e28b97230b') OR title = 'LOẠN THẾ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('f9a22933b9bd527b8b9370e28b97230b'), 'LOẠN THẾ', NULL, 99, '2027-02-06', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/02-2027/loan-the-poster.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_52 = (SELECT id FROM movies WHERE title = 'LOẠN THẾ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_52 AND genre_id = @cinestar_seed_genre_07);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_52, @cinestar_seed_genre_07 FROM DUAL
WHERE @cinestar_seed_movie_52 IS NOT NULL AND @cinestar_seed_genre_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/db625d7d-2a0b-455a-891c-2410a9e62938/
-- Movie: QUÝ TỬ
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('bbaebad5d2fa51c98d5ef3da76e6e6cc') OR title = 'QUÝ TỬ');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('bbaebad5d2fa51c98d5ef3da76e6e6cc'), 'QUÝ TỬ', NULL, 99, '2027-02-06', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/02-2027/quy-tu-teaser.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_53 = (SELECT id FROM movies WHERE title = 'QUÝ TỬ' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_53 AND genre_id = @cinestar_seed_genre_17);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_53, @cinestar_seed_genre_17 FROM DUAL
WHERE @cinestar_seed_movie_53 IS NOT NULL AND @cinestar_seed_genre_17 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_53 AND genre_id = @cinestar_seed_genre_04);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_53, @cinestar_seed_genre_04 FROM DUAL
WHERE @cinestar_seed_movie_53 IS NOT NULL AND @cinestar_seed_genre_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;

-- Source: https://cinestar.com.vn/movie/3b8d1a09-8a67-4bb5-add3-463bd8c83107/
-- Movie: ÁN MẠNG KARAOKE (T16)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movies WHERE id = UNHEX('aeb720fe135b5aaea0bb3b23453e8d50') OR title = 'ÁN MẠNG KARAOKE (T16)');
INSERT INTO movies (id, title, description, duration_minutes, release_date, poster_url, trailer_url, status, version, created_at, updated_at)
SELECT UNHEX('aeb720fe135b5aaea0bb3b23453e8d50'), 'ÁN MẠNG KARAOKE (T16)', NULL, 96, '2026-10-30', 'https://api-website.cinestar.com.vn/media/wysiwyg/Posters/10-2026/an-mang-new.jpg', NULL, 'UPCOMING', 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_movie_54 = (SELECT id FROM movies WHERE title = 'ÁN MẠNG KARAOKE (T16)' LIMIT 1);
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM movie_genres WHERE movie_id = @cinestar_seed_movie_54 AND genre_id = @cinestar_seed_genre_06);
INSERT INTO movie_genres (movie_id, genre_id)
SELECT @cinestar_seed_movie_54, @cinestar_seed_genre_06 FROM DUAL
WHERE @cinestar_seed_movie_54 IS NOT NULL AND @cinestar_seed_genre_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;

USE cinema_inventory_db;

-- 3. CINEMAS
-- Source: https://cinestar.com.vn/book-tickets/7095da31-3c05-4bc5-bc28-fef354d9c1f2/
-- Cinema: Cinestar Hiệp Phú (TP.HCM)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('d23678c6d0f050499b972e7d8563d859') OR (name = 'Cinestar Hiệp Phú (TP.HCM)' AND address = 'Trung tâm MM Mega Market Hiệp Phú, số 2 đường Trương Thị Hoa, Khu phố 3, Phường Tân Thới Hiệp, Thành phố Hồ Chí Minh, Việt Nam'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('d23678c6d0f050499b972e7d8563d859'), 'Cinestar Hiệp Phú (TP.HCM)', 'Trung tâm MM Mega Market Hiệp Phú, số 2 đường Trương Thị Hoa, Khu phố 3, Phường Tân Thới Hiệp, Thành phố Hồ Chí Minh, Việt Nam', 'Hồ Chí Minh', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_01 = (SELECT id FROM cinemas WHERE name = 'Cinestar Hiệp Phú (TP.HCM)' AND address = 'Trung tâm MM Mega Market Hiệp Phú, số 2 đường Trương Thị Hoa, Khu phố 3, Phường Tân Thới Hiệp, Thành phố Hồ Chí Minh, Việt Nam' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/8f3a5832-8340-4a43-89bc-6653817162f1/
-- Cinema: Cinestar Quốc Thanh (TP.HCM)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('475ea861a4d55b4a87dc9df6c1853c92') OR (name = 'Cinestar Quốc Thanh (TP.HCM)' AND address = '271 Nguyễn Trãi, Phường Cầu Ông Lãnh, Thành Phố Hồ Chí Minh'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('475ea861a4d55b4a87dc9df6c1853c92'), 'Cinestar Quốc Thanh (TP.HCM)', '271 Nguyễn Trãi, Phường Cầu Ông Lãnh, Thành Phố Hồ Chí Minh', 'Hồ Chí Minh', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_02 = (SELECT id FROM cinemas WHERE name = 'Cinestar Quốc Thanh (TP.HCM)' AND address = '271 Nguyễn Trãi, Phường Cầu Ông Lãnh, Thành Phố Hồ Chí Minh' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/85e300f7-6aa7-48bc-b29f-405255918bba/
-- Cinema: Cinestar Parkcity Hà Nội
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('d60a2e73cefa56e2b1fef6c86a9f9121') OR (name = 'Cinestar Parkcity Hà Nội' AND address = 'Tầng 3TTTM The LinC, Khu đô thị ParkCity Hà Nội, 165 Lê Trọng Tấn, P. Dương Nội, Hà Nội'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('d60a2e73cefa56e2b1fef6c86a9f9121'), 'Cinestar Parkcity Hà Nội', 'Tầng 3TTTM The LinC, Khu đô thị ParkCity Hà Nội, 165 Lê Trọng Tấn, P. Dương Nội, Hà Nội', 'Hà Nội', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_03 = (SELECT id FROM cinemas WHERE name = 'Cinestar Parkcity Hà Nội' AND address = 'Tầng 3TTTM The LinC, Khu đô thị ParkCity Hà Nội, 165 Lê Trọng Tấn, P. Dương Nội, Hà Nội' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/cf13e1ce-2c1f-4c73-8ce5-7ef65472db3c/
-- Cinema: Cinestar Sinh Viên (TP.HCM)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('165904a6ca5152f8b17b840104fb7c26') OR (name = 'Cinestar Sinh Viên (TP.HCM)' AND address = 'Nhà văn hóa sinh viên - Đại học Quốc gia HCM, P. Đông Hòa, TP. HCM'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('165904a6ca5152f8b17b840104fb7c26'), 'Cinestar Sinh Viên (TP.HCM)', 'Nhà văn hóa sinh viên - Đại học Quốc gia HCM, P. Đông Hòa, TP. HCM', 'Hồ Chí Minh', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_04 = (SELECT id FROM cinemas WHERE name = 'Cinestar Sinh Viên (TP.HCM)' AND address = 'Nhà văn hóa sinh viên - Đại học Quốc gia HCM, P. Đông Hòa, TP. HCM' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/f8a60463-5c34-49a9-9ae8-52081e387bb8/
-- Cinema: Cinestar Huế (TP. Huế)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('5df9fcba59365891a27a8f3a8dc26a15') OR (name = 'Cinestar Huế (TP. Huế)' AND address = '25 Hai Bà Trưng, Phường Thuận Hoá, TP. Huế'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('5df9fcba59365891a27a8f3a8dc26a15'), 'Cinestar Huế (TP. Huế)', '25 Hai Bà Trưng, Phường Thuận Hoá, TP. Huế', 'Huế', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_05 = (SELECT id FROM cinemas WHERE name = 'Cinestar Huế (TP. Huế)' AND address = '25 Hai Bà Trưng, Phường Thuận Hoá, TP. Huế' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/4a51b9ee-f143-4411-9dbb-5f54a1c382c0/
-- Cinema: Cinestar Kiên Giang (Rạch Sỏi)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('0b8c55cade4b597d94de19270292721d') OR (name = 'Cinestar Kiên Giang (Rạch Sỏi)' AND address = 'Lô A2 - Khu 2 Trung tâm Thương mại Rạch Sỏi, Đường Nguyễn Chí Thanh, Phường Rạch Sỏi, Thành phố Rạch Giá, Tỉnh Kiên Giang'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('0b8c55cade4b597d94de19270292721d'), 'Cinestar Kiên Giang (Rạch Sỏi)', 'Lô A2 - Khu 2 Trung tâm Thương mại Rạch Sỏi, Đường Nguyễn Chí Thanh, Phường Rạch Sỏi, Thành phố Rạch Giá, Tỉnh Kiên Giang', 'Kiên Giang', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_06 = (SELECT id FROM cinemas WHERE name = 'Cinestar Kiên Giang (Rạch Sỏi)' AND address = 'Lô A2 - Khu 2 Trung tâm Thương mại Rạch Sỏi, Đường Nguyễn Chí Thanh, Phường Rạch Sỏi, Thành phố Rạch Giá, Tỉnh Kiên Giang' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/e08f986a-1937-419e-b1b1-759b7c74728b/
-- Cinema: Cinestar Đà Lạt (Lâm Đồng)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('e3600deecd7f536aab28250b76f0fcb9') OR (name = 'Cinestar Đà Lạt (Lâm Đồng)' AND address = 'Quảng trường Lâm Viên, Phường Xuân Hương - Đà Lạt, tỉnh Lâm Đồng'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('e3600deecd7f536aab28250b76f0fcb9'), 'Cinestar Đà Lạt (Lâm Đồng)', 'Quảng trường Lâm Viên, Phường Xuân Hương - Đà Lạt, tỉnh Lâm Đồng', 'Đà Lạt', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_07 = (SELECT id FROM cinemas WHERE name = 'Cinestar Đà Lạt (Lâm Đồng)' AND address = 'Quảng trường Lâm Viên, Phường Xuân Hương - Đà Lạt, tỉnh Lâm Đồng' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/104509be-034e-47c1-bf1b-aba7f2df4f28/
-- Cinema: Cinestar Lâm Đồng (Đức Trọng)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('9f435e8818805c0e95482cc238b2f351') OR (name = 'Cinestar Lâm Đồng (Đức Trọng)' AND address = 'Tầng 4, Trung tâm Thương mại và Dịch vụ tài chính Sacombank, 713 Quốc lộ 20, Xã Đức Trọng, Tỉnh Lâm Đồng'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('9f435e8818805c0e95482cc238b2f351'), 'Cinestar Lâm Đồng (Đức Trọng)', 'Tầng 4, Trung tâm Thương mại và Dịch vụ tài chính Sacombank, 713 Quốc lộ 20, Xã Đức Trọng, Tỉnh Lâm Đồng', 'Lâm Đồng', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_08 = (SELECT id FROM cinemas WHERE name = 'Cinestar Lâm Đồng (Đức Trọng)' AND address = 'Tầng 4, Trung tâm Thương mại và Dịch vụ tài chính Sacombank, 713 Quốc lộ 20, Xã Đức Trọng, Tỉnh Lâm Đồng' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/8f54df74-3796-42ea-896e-cd638eec1fe3/
-- Cinema: Cinestar Mỹ Tho (Đồng Tháp)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('38a221a43f3f50cbb13f2a231ef6bf4b') OR (name = 'Cinestar Mỹ Tho (Đồng Tháp)' AND address = '52 Đinh Bộ Lĩnh, Phường Mỹ Tho, tỉnh Đồng Tháp'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('38a221a43f3f50cbb13f2a231ef6bf4b'), 'Cinestar Mỹ Tho (Đồng Tháp)', '52 Đinh Bộ Lĩnh, Phường Mỹ Tho, tỉnh Đồng Tháp', 'Đồng Tháp', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_09 = (SELECT id FROM cinemas WHERE name = 'Cinestar Mỹ Tho (Đồng Tháp)' AND address = '52 Đinh Bộ Lĩnh, Phường Mỹ Tho, tỉnh Đồng Tháp' ORDER BY created_at, id LIMIT 1);

-- Source: https://cinestar.com.vn/book-tickets/42bec658-2331-4dc7-ac03-39231c069d7e/
-- Cinema: Cinestar Satra Quận 6 (TP.HCM)
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM cinemas WHERE id = UNHEX('9219116ef48e5d619d743c5f16335336') OR (name = 'Cinestar Satra Quận 6 (TP.HCM)' AND address = 'Tầng 6, TTTM Centre Mall, 1466 Võ Văn Kiệt, Phường Bình Tiên, TP. HCM'));
INSERT INTO cinemas (id, name, address, city, active, version, created_at, updated_at)
SELECT UNHEX('9219116ef48e5d619d743c5f16335336'), 'Cinestar Satra Quận 6 (TP.HCM)', 'Tầng 6, TTTM Centre Mall, 1466 Võ Văn Kiệt, Phường Bình Tiên, TP. HCM', 'Hồ Chí Minh', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_exists = 0;
SET @cinestar_seed_cinema_10 = (SELECT id FROM cinemas WHERE name = 'Cinestar Satra Quận 6 (TP.HCM)' AND address = 'Tầng 6, TTTM Centre Mall, 1466 Võ Văn Kiệt, Phường Bình Tiên, TP. HCM' ORDER BY created_at, id LIMIT 1);

-- 4. VERIFIED STANDARD ROOMS; KHONG tao seats tu tong suc chua rap.
-- Cinema: Cinestar Lâm Đồng (Đức Trọng); source room_id: 109; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('7af0ea015ffe5c5e884007203c464664') OR (cinema_id = @cinestar_seed_cinema_08 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('7af0ea015ffe5c5e884007203c464664'), @cinestar_seed_cinema_08, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_08 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_01 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_08 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Lâm Đồng (Đức Trọng); source room_id: 110; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('a49a3a283908580a8a5b02b1616e5970') OR (cinema_id = @cinestar_seed_cinema_08 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('a49a3a283908580a8a5b02b1616e5970'), @cinestar_seed_cinema_08, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_08 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_02 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_08 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Lâm Đồng (Đức Trọng); source room_id: 111; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('92d420891fa05854a2d8514e009c1467') OR (cinema_id = @cinestar_seed_cinema_08 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('92d420891fa05854a2d8514e009c1467'), @cinestar_seed_cinema_08, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_08 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_03 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_08 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Lâm Đồng (Đức Trọng); source room_id: 112; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('7efcdd39b3a9557fb9382a192c949c73') OR (cinema_id = @cinestar_seed_cinema_08 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('7efcdd39b3a9557fb9382a192c949c73'), @cinestar_seed_cinema_08, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_08 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_04 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_08 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Satra Quận 6 (TP.HCM); source room_id: 114; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('00a89fd511df5021a60b1ff25682c58f') OR (cinema_id = @cinestar_seed_cinema_10 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('00a89fd511df5021a60b1ff25682c58f'), @cinestar_seed_cinema_10, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_05 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_10 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Satra Quận 6 (TP.HCM); source room_id: 115; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('45f1d698e4735f6abf94fe0f74e2dbce') OR (cinema_id = @cinestar_seed_cinema_10 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('45f1d698e4735f6abf94fe0f74e2dbce'), @cinestar_seed_cinema_10, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_06 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_10 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Satra Quận 6 (TP.HCM); source room_id: 116; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('572fafc4046457898d15daaedd55a9c0') OR (cinema_id = @cinestar_seed_cinema_10 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('572fafc4046457898d15daaedd55a9c0'), @cinestar_seed_cinema_10, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_07 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_10 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Satra Quận 6 (TP.HCM); source room_id: 117; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('ed85360144275f97acff148bfb93f6e1') OR (cinema_id = @cinestar_seed_cinema_10 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('ed85360144275f97acff148bfb93f6e1'), @cinestar_seed_cinema_10, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_08 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_10 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Satra Quận 6 (TP.HCM); source room_id: 119; room_name: 07
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('61442b3772455a27857acff366b1a92e') OR (cinema_id = @cinestar_seed_cinema_10 AND name = '07'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('61442b3772455a27857acff366b1a92e'), @cinestar_seed_cinema_10, '07', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_10 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_09 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_10 AND name = '07' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Kiên Giang (Rạch Sỏi); source room_id: 105; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('43cbb3a41c5e514aae58119abceaba00') OR (cinema_id = @cinestar_seed_cinema_06 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('43cbb3a41c5e514aae58119abceaba00'), @cinestar_seed_cinema_06, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_10 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_06 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Kiên Giang (Rạch Sỏi); source room_id: 106; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('4903d700b91059f7ada029e53359c233') OR (cinema_id = @cinestar_seed_cinema_06 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('4903d700b91059f7ada029e53359c233'), @cinestar_seed_cinema_06, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_11 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_06 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Kiên Giang (Rạch Sỏi); source room_id: 107; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('73ac9e7479085000ba90271296897aba') OR (cinema_id = @cinestar_seed_cinema_06 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('73ac9e7479085000ba90271296897aba'), @cinestar_seed_cinema_06, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_12 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_06 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Kiên Giang (Rạch Sỏi); source room_id: 108; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('ff6591f3df355cb5b23500a4fcef8741') OR (cinema_id = @cinestar_seed_cinema_06 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('ff6591f3df355cb5b23500a4fcef8741'), @cinestar_seed_cinema_06, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_06 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_13 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_06 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Hiệp Phú (TP.HCM); source room_id: 125; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('516f857603d858619fc437749616f674') OR (cinema_id = @cinestar_seed_cinema_01 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('516f857603d858619fc437749616f674'), @cinestar_seed_cinema_01, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_01 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_14 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_01 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Hiệp Phú (TP.HCM); source room_id: 126; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('969583c58fb85f51863df25b59357689') OR (cinema_id = @cinestar_seed_cinema_01 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('969583c58fb85f51863df25b59357689'), @cinestar_seed_cinema_01, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_01 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_15 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_01 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Hiệp Phú (TP.HCM); source room_id: 127; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('f919d9d5944a5524bc0a0d7707c7123f') OR (cinema_id = @cinestar_seed_cinema_01 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('f919d9d5944a5524bc0a0d7707c7123f'), @cinestar_seed_cinema_01, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_01 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_16 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_01 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Hiệp Phú (TP.HCM); source room_id: 128; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('788371ef94f1538bb81c7cddcf54e7d3') OR (cinema_id = @cinestar_seed_cinema_01 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('788371ef94f1538bb81c7cddcf54e7d3'), @cinestar_seed_cinema_01, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_01 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_17 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_01 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Parkcity Hà Nội; source room_id: 120; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('23b4e0cb0a475a1b926d05c0517b914e') OR (cinema_id = @cinestar_seed_cinema_03 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('23b4e0cb0a475a1b926d05c0517b914e'), @cinestar_seed_cinema_03, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_03 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_18 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_03 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Parkcity Hà Nội; source room_id: 121; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('8c1b5e73bea45827882a2331e2d23e31') OR (cinema_id = @cinestar_seed_cinema_03 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('8c1b5e73bea45827882a2331e2d23e31'), @cinestar_seed_cinema_03, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_03 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_19 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_03 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Parkcity Hà Nội; source room_id: 123; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('da6a6546bbd35b8891cb5154f9a246dc') OR (cinema_id = @cinestar_seed_cinema_03 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('da6a6546bbd35b8891cb5154f9a246dc'), @cinestar_seed_cinema_03, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_03 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_20 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_03 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Parkcity Hà Nội; source room_id: 124; room_name: 06
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('585c83051b0f5c61a1040073fbcd6e23') OR (cinema_id = @cinestar_seed_cinema_03 AND name = '06'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('585c83051b0f5c61a1040073fbcd6e23'), @cinestar_seed_cinema_03, '06', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_03 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_21 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_03 AND name = '06' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Quốc Thanh (TP.HCM); source room_id: 61; room_name: 06
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('199eebf80cc156bbbc38cec6c7488596') OR (cinema_id = @cinestar_seed_cinema_02 AND name = '06'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('199eebf80cc156bbbc38cec6c7488596'), @cinestar_seed_cinema_02, '06', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_22 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_02 AND name = '06' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Quốc Thanh (TP.HCM); source room_id: 63; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('75175e44310356b697054e8ff8d55d05') OR (cinema_id = @cinestar_seed_cinema_02 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('75175e44310356b697054e8ff8d55d05'), @cinestar_seed_cinema_02, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_23 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_02 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Quốc Thanh (TP.HCM); source room_id: 64; room_name: 04
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1') OR (cinema_id = @cinestar_seed_cinema_02 AND name = '04'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), @cinestar_seed_cinema_02, '04', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_24 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_02 AND name = '04' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Quốc Thanh (TP.HCM); source room_id: 65; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('886509a2634e5d5bb3ab334aa6b46522') OR (cinema_id = @cinestar_seed_cinema_02 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('886509a2634e5d5bb3ab334aa6b46522'), @cinestar_seed_cinema_02, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_25 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_02 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Quốc Thanh (TP.HCM); source room_id: 67; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('cff6b965a896555b9865a11dad706054') OR (cinema_id = @cinestar_seed_cinema_02 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('cff6b965a896555b9865a11dad706054'), @cinestar_seed_cinema_02, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_02 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_26 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_02 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Mỹ Tho (Đồng Tháp); source room_id: 100; room_name: 06
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('3bfdbe02c1d65ecab4b6414580c216bd') OR (cinema_id = @cinestar_seed_cinema_09 AND name = '06'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('3bfdbe02c1d65ecab4b6414580c216bd'), @cinestar_seed_cinema_09, '06', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_27 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_09 AND name = '06' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Mỹ Tho (Đồng Tháp); source room_id: 102; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('cd2c0f5e767358838a54434c7321b273') OR (cinema_id = @cinestar_seed_cinema_09 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('cd2c0f5e767358838a54434c7321b273'), @cinestar_seed_cinema_09, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_28 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_09 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Mỹ Tho (Đồng Tháp); source room_id: 96; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('de9af3277b2a516e8ab4d4e451cdb268') OR (cinema_id = @cinestar_seed_cinema_09 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('de9af3277b2a516e8ab4d4e451cdb268'), @cinestar_seed_cinema_09, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_29 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_09 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Mỹ Tho (Đồng Tháp); source room_id: 97; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('96fe92ab63115beb83bf12749196a809') OR (cinema_id = @cinestar_seed_cinema_09 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('96fe92ab63115beb83bf12749196a809'), @cinestar_seed_cinema_09, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_30 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_09 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Mỹ Tho (Đồng Tháp); source room_id: 99; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('91f9fda13de2597b82882dcc57225d72') OR (cinema_id = @cinestar_seed_cinema_09 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('91f9fda13de2597b82882dcc57225d72'), @cinestar_seed_cinema_09, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_09 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_31 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_09 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Sinh Viên (TP.HCM); source room_id: 83; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('eaf35007c7a65708bcfbde82d49f7597') OR (cinema_id = @cinestar_seed_cinema_04 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('eaf35007c7a65708bcfbde82d49f7597'), @cinestar_seed_cinema_04, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_32 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_04 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Sinh Viên (TP.HCM); source room_id: 84; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('b3aadcb832aa59be9aa60a6f6df76438') OR (cinema_id = @cinestar_seed_cinema_04 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('b3aadcb832aa59be9aa60a6f6df76438'), @cinestar_seed_cinema_04, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_33 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_04 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Sinh Viên (TP.HCM); source room_id: 85; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('78f1e018623d51a78d7959b803085e36') OR (cinema_id = @cinestar_seed_cinema_04 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('78f1e018623d51a78d7959b803085e36'), @cinestar_seed_cinema_04, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_34 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_04 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Sinh Viên (TP.HCM); source room_id: 86; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('c3fb732e28e05c28a795970d9ef9d5a7') OR (cinema_id = @cinestar_seed_cinema_04 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('c3fb732e28e05c28a795970d9ef9d5a7'), @cinestar_seed_cinema_04, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_04 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_35 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_04 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 103; room_name: 7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('6e36f0cad4995c079ef1144a738cc3e6') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '7'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('6e36f0cad4995c079ef1144a738cc3e6'), @cinestar_seed_cinema_07, '7', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_36 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '7' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 76; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('fe2d5517a8c558f6a57174e64e53012e') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('fe2d5517a8c558f6a57174e64e53012e'), @cinestar_seed_cinema_07, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_37 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 77; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('c986e18444bf541ba751e1dc078fd6b8') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('c986e18444bf541ba751e1dc078fd6b8'), @cinestar_seed_cinema_07, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_38 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 78; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('6703ed9072865708a7b02b7955415bc9') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('6703ed9072865708a7b02b7955415bc9'), @cinestar_seed_cinema_07, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_39 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 79; room_name: 05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('51497f7d1b355681b7196361e9a1f92f') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '05'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('51497f7d1b355681b7196361e9a1f92f'), @cinestar_seed_cinema_07, '05', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_40 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '05' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Đà Lạt (Lâm Đồng); source room_id: 80; room_name: 06
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('c2a1f0ef2675552ca9be1154b341d473') OR (cinema_id = @cinestar_seed_cinema_07 AND name = '06'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('c2a1f0ef2675552ca9be1154b341d473'), @cinestar_seed_cinema_07, '06', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_07 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_41 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_07 AND name = '06' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 87; room_name: 01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('36d33e9eaffd573689f5d3d04be85b76') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '01'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('36d33e9eaffd573689f5d3d04be85b76'), @cinestar_seed_cinema_05, '01', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_42 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '01' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 88; room_name: 02
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('eca8df8bd1005028bf5f614b7246318e') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '02'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('eca8df8bd1005028bf5f614b7246318e'), @cinestar_seed_cinema_05, '02', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_43 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '02' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 89; room_name: 03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('806182ba029f57e4a51767636757d88a') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '03'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('806182ba029f57e4a51767636757d88a'), @cinestar_seed_cinema_05, '03', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_44 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '03' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 91; room_name: 06
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('90b916dbdc085594a35011c94e290fb3') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '06'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('90b916dbdc085594a35011c94e290fb3'), @cinestar_seed_cinema_05, '06', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_45 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '06' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 92; room_name: 07
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('4d6ed45327295962b32ecc047d67049d') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '07'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('4d6ed45327295962b32ecc047d67049d'), @cinestar_seed_cinema_05, '07', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_46 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '07' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 93; room_name: 08
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('58a4c58ba811531188a7282262f91bfc') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '08'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('58a4c58ba811531188a7282262f91bfc'), @cinestar_seed_cinema_05, '08', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_47 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '08' AND room_type = 'STANDARD' LIMIT 1);

-- Cinema: Cinestar Huế (TP. Huế); source room_id: 94; room_name: 09
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM rooms WHERE id = UNHEX('9dd4451195f45e52819fce3cc3debe10') OR (cinema_id = @cinestar_seed_cinema_05 AND name = '09'));
INSERT INTO rooms (id, cinema_id, name, room_type, active, version, created_at, updated_at)
SELECT UNHEX('9dd4451195f45e52819fce3cc3debe10'), @cinestar_seed_cinema_05, '09', 'STANDARD', TRUE, 0, @cinestar_seed_now, @cinestar_seed_now FROM DUAL
WHERE @cinestar_seed_cinema_05 IS NOT NULL
  AND @cinestar_seed_exists = 0;
SET @cinestar_seed_room_48 = (SELECT id FROM rooms WHERE cinema_id = @cinestar_seed_cinema_05 AND name = '09' AND room_type = 'STANDARD' LIMIT 1);

-- 5. VERIFIED FUTURE SHOWTIMES (SCHEDULED, no seat inventory, no ticket price).
-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 10:25:00 +0700
-- source showtime_id: 56edfc02-12db-4366-b9a3-390c6d1204cd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('028627c94d3d5c4aa4ff39ff614745e3') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 03:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:20:00' AND ends_at > '2026-10-05 03:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('028627c94d3d5c4aa4ff39ff614745e3'), @cinestar_seed_movie_07, @cinestar_seed_room_01, '2026-10-05 03:25:00', '2026-10-05 05:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 12:40:00 +0700
-- source showtime_id: faba62b0-627a-451c-ba83-052c792ec928
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('14157c11e16659b5b49d452bc133893c') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 05:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:00:00' AND ends_at > '2026-10-05 05:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('14157c11e16659b5b49d452bc133893c'), @cinestar_seed_movie_08, @cinestar_seed_room_01, '2026-10-05 05:40:00', '2026-10-05 07:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 14:20:00 +0700
-- source showtime_id: 20d8c5fa-2b89-425c-8b38-dfc06d11ad07
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f63ae219128c5c64ab8af716febc930e') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:56:00' AND ends_at > '2026-10-05 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f63ae219128c5c64ab8af716febc930e'), @cinestar_seed_movie_06, @cinestar_seed_room_01, '2026-10-05 07:20:00', '2026-10-05 08:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 16:20:00 +0700
-- source showtime_id: b5c56c2a-57ca-411a-859c-8654f25e78ea
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('be0da291d62357fdba10cee422ef2397') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:01:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('be0da291d62357fdba10cee422ef2397'), @cinestar_seed_movie_29, @cinestar_seed_room_01, '2026-10-05 09:20:00', '2026-10-05 11:01:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 18:25:00 +0700
-- source showtime_id: 82d2b8af-0386-4fb0-a0f9-57ecb964814a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('83d6387de2785d1ea1dfccb4051acfa3') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 11:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:40:00' AND ends_at > '2026-10-05 11:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('83d6387de2785d1ea1dfccb4051acfa3'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-05 11:25:00', '2026-10-05 13:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-05 21:05:00 +0700
-- source showtime_id: fc9a6102-d100-40d2-a45f-a6b90e9bfa94
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4e1c69a05f2a583db92ecbbcecd94d19') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-05 14:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:20:00' AND ends_at > '2026-10-05 14:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4e1c69a05f2a583db92ecbbcecd94d19'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-05 14:05:00', '2026-10-05 16:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 08:00:00 +0700
-- source showtime_id: dcdf2e09-0234-4415-8a4e-e7c4ba476734
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6ce8924a511356a3b4e5363520cb3c67') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:02:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6ce8924a511356a3b4e5363520cb3c67'), @cinestar_seed_movie_05, @cinestar_seed_room_01, '2026-10-06 01:00:00', '2026-10-06 03:02:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 10:25:00 +0700
-- source showtime_id: ff79bc58-4322-441a-8338-e140dff4cd8d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d402ac78eb905ea1bb06d7e135d7aeb5') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 03:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:20:00' AND ends_at > '2026-10-06 03:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d402ac78eb905ea1bb06d7e135d7aeb5'), @cinestar_seed_movie_07, @cinestar_seed_room_01, '2026-10-06 03:25:00', '2026-10-06 05:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 12:40:00 +0700
-- source showtime_id: c80538ce-db3b-494e-bfd2-f32120ce248c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8964af3aa456572b83478235af391860') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 05:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:00:00' AND ends_at > '2026-10-06 05:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8964af3aa456572b83478235af391860'), @cinestar_seed_movie_08, @cinestar_seed_room_01, '2026-10-06 05:40:00', '2026-10-06 07:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 14:20:00 +0700
-- source showtime_id: c6f3858b-8a03-4549-9aee-611064f4b380
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('14855f03536f541a8a2e3b290e5dd652') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:56:00' AND ends_at > '2026-10-06 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('14855f03536f541a8a2e3b290e5dd652'), @cinestar_seed_movie_06, @cinestar_seed_room_01, '2026-10-06 07:20:00', '2026-10-06 08:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 16:20:00 +0700
-- source showtime_id: ae4fc299-6acd-438d-954b-b9e5a378a400
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b1631e1153ff580eaa6b2f070e26d7dd') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:01:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b1631e1153ff580eaa6b2f070e26d7dd'), @cinestar_seed_movie_29, @cinestar_seed_room_01, '2026-10-06 09:20:00', '2026-10-06 11:01:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 18:25:00 +0700
-- source showtime_id: 5825b2d9-5135-4b59-bd0f-097e36ce0c7f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7e999195a2a2515faaa672588381e3b7') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 11:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:40:00' AND ends_at > '2026-10-06 11:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7e999195a2a2515faaa672588381e3b7'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-06 11:25:00', '2026-10-06 13:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-06 21:05:00 +0700
-- source showtime_id: 1bf3e657-5b4f-4f4f-af56-ca0b40369719
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c074b1fe0b0754fa852fe7ebd1fc98c1') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-06 14:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:20:00' AND ends_at > '2026-10-06 14:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c074b1fe0b0754fa852fe7ebd1fc98c1'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-06 14:05:00', '2026-10-06 16:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 08:55:00 +0700
-- source showtime_id: 891fc339-55f2-4dce-afc7-c8aeb6978527
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d1ecacf4800a594d814f82308bb121cd') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 01:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:50:00' AND ends_at > '2026-10-07 01:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d1ecacf4800a594d814f82308bb121cd'), @cinestar_seed_movie_04, @cinestar_seed_room_01, '2026-10-07 01:55:00', '2026-10-07 03:50:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 11:15:00 +0700
-- source showtime_id: 59b3a0d7-4555-4538-8339-d5d9a023e2c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('50921ce3e3f55216ab20c0c45142624b') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 04:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 05:35:00' AND ends_at > '2026-10-07 04:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('50921ce3e3f55216ab20c0c45142624b'), @cinestar_seed_movie_08, @cinestar_seed_room_01, '2026-10-07 04:15:00', '2026-10-07 05:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 13:00:00 +0700
-- source showtime_id: 7c877c94-6122-4455-a24c-696d5e847f19
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03cd8959fadb5abfb442dfc78df47339') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:41:00' AND ends_at > '2026-10-07 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03cd8959fadb5abfb442dfc78df47339'), @cinestar_seed_movie_29, @cinestar_seed_room_01, '2026-10-07 06:00:00', '2026-10-07 07:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 15:05:00 +0700
-- source showtime_id: 9921cd90-fefe-4b2d-bf19-472978a63e36
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d738e593ded350c2a1b1e979d97b4d61') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 08:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:41:00' AND ends_at > '2026-10-07 08:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d738e593ded350c2a1b1e979d97b4d61'), @cinestar_seed_movie_06, @cinestar_seed_room_01, '2026-10-07 08:05:00', '2026-10-07 09:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 17:05:00 +0700
-- source showtime_id: c6eb87b7-f853-474a-9f0e-871bd10edf1e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('81a6d52b56265896b167e7d7307bef85') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 10:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:20:00' AND ends_at > '2026-10-07 10:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('81a6d52b56265896b167e7d7307bef85'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-07 10:05:00', '2026-10-07 12:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 19:45:00 +0700
-- source showtime_id: 0c37cc74-0ce5-4ddd-97b9-0e311f911115
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ee9f5a098de95dddbf6831e8f3c9e104') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 12:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:00:00' AND ends_at > '2026-10-07 12:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ee9f5a098de95dddbf6831e8f3c9e104'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-07 12:45:00', '2026-10-07 15:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-07 22:25:00 +0700
-- source showtime_id: 52323add-47b9-4e78-a460-39d0e5da7b66
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('df5941a47e8656ac91d8c10514120f22') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-07 15:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 17:40:00' AND ends_at > '2026-10-07 15:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('df5941a47e8656ac91d8c10514120f22'), @cinestar_seed_movie_15, @cinestar_seed_room_01, '2026-10-07 15:25:00', '2026-10-07 17:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-12 20:30:00 +0700
-- source showtime_id: 83bd35f6-67ed-4b0a-8e5b-5d9996ec18ee
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c06ba426159655b28a424bd2c6a2bf96') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-12 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-12 15:08:00' AND ends_at > '2026-10-12 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c06ba426159655b28a424bd2c6a2bf96'), @cinestar_seed_movie_12, @cinestar_seed_room_01, '2026-10-12 13:30:00', '2026-10-12 15:08:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-12 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-14 20:30:00 +0700
-- source showtime_id: d6159952-8399-4ad4-a673-0607296d30db
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9b0ded3c67435728bcfd88868945f04a') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-14 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-14 15:08:00' AND ends_at > '2026-10-14 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9b0ded3c67435728bcfd88868945f04a'), @cinestar_seed_movie_12, @cinestar_seed_room_01, '2026-10-14 13:30:00', '2026-10-14 15:08:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-14 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Lâm Đồng (Đức Trọng); room 05; local 2026-10-17 20:30:00 +0700
-- source showtime_id: bea42356-2635-46b7-900f-ca4156e09663
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0e4ca8e6b2165d66ab12aae930071419') OR (room_id = @cinestar_seed_room_01 AND (starts_at = '2026-10-17 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-17 15:08:00' AND ends_at > '2026-10-17 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0e4ca8e6b2165d66ab12aae930071419'), @cinestar_seed_movie_12, @cinestar_seed_room_01, '2026-10-17 13:30:00', '2026-10-17 15:08:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_01 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-17 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-05 12:30:00 +0700
-- source showtime_id: e2803339-0ef5-4098-b4b3-23e467ce3dca
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5b58a17d44285c6daf101ca8b57e3609') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-05 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:45:00' AND ends_at > '2026-10-05 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5b58a17d44285c6daf101ca8b57e3609'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-05 05:30:00', '2026-10-05 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-05 15:10:00 +0700
-- source showtime_id: 2b153291-4957-4a63-8caa-19f7f2eddda9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3c35309882df544e87b59d6b9c80939c') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-05 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:25:00' AND ends_at > '2026-10-05 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3c35309882df544e87b59d6b9c80939c'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-05 08:10:00', '2026-10-05 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-05 17:50:00 +0700
-- source showtime_id: a5d638ae-49fa-405a-9611-51eb88763349
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a6e3237deeff5cdaa537308e1bb7b4dc') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-05 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:05:00' AND ends_at > '2026-10-05 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a6e3237deeff5cdaa537308e1bb7b4dc'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-05 10:50:00', '2026-10-05 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-05 20:30:00 +0700
-- source showtime_id: 7dfd249d-cbbb-4767-aaf4-cf2b63c4721e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4606615f99cd56c8b5d3e0aa1c05aac0') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-05 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:45:00' AND ends_at > '2026-10-05 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4606615f99cd56c8b5d3e0aa1c05aac0'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-05 13:30:00', '2026-10-05 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-06 09:50:00 +0700
-- source showtime_id: c90c536c-5e59-4337-b480-a61d1edc5e2a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('edcb17e12c5e57d3a74c89e084f4d937') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-06 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:05:00' AND ends_at > '2026-10-06 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('edcb17e12c5e57d3a74c89e084f4d937'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-06 02:50:00', '2026-10-06 05:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-06 12:30:00 +0700
-- source showtime_id: c3fd6d12-ea18-4518-a481-e549a92d789b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5af4bf860d3f5a47a185a7245f153662') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-06 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:45:00' AND ends_at > '2026-10-06 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5af4bf860d3f5a47a185a7245f153662'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-06 05:30:00', '2026-10-06 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-06 15:10:00 +0700
-- source showtime_id: f6b606ae-a49d-40b2-8150-15adae839966
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('018f6eef5a2a56ee8a24467ed89185a9') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-06 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:25:00' AND ends_at > '2026-10-06 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('018f6eef5a2a56ee8a24467ed89185a9'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-06 08:10:00', '2026-10-06 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-06 17:50:00 +0700
-- source showtime_id: bf22e346-1a65-4f88-8a27-31e79e2128e7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a171ca5b2d6f537aafb27c5021b2c595') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-06 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:05:00' AND ends_at > '2026-10-06 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a171ca5b2d6f537aafb27c5021b2c595'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-06 10:50:00', '2026-10-06 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-06 20:30:00 +0700
-- source showtime_id: 186a162d-8973-485b-801c-0740da260019
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('44241641d7765cdd9b21aefd81920c50') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-06 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:45:00' AND ends_at > '2026-10-06 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('44241641d7765cdd9b21aefd81920c50'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-06 13:30:00', '2026-10-06 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 08:20:00 +0700
-- source showtime_id: 1c4d3f22-f9b9-4414-aa12-61eb89aba882
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ab7c0f9ab6225ce5ab4337a468dd8f05') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 01:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:35:00' AND ends_at > '2026-10-07 01:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ab7c0f9ab6225ce5ab4337a468dd8f05'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 01:20:00', '2026-10-07 03:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 11:00:00 +0700
-- source showtime_id: 92e74347-1c15-46a1-91e7-437895d7faf2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ff537d9aa4ab5ff38f2c40bbc5fab361') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:15:00' AND ends_at > '2026-10-07 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ff537d9aa4ab5ff38f2c40bbc5fab361'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 04:00:00', '2026-10-07 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 13:40:00 +0700
-- source showtime_id: ea91c47f-ebac-4d56-9cae-26ae8e353aa5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c04a05a465fb52fb812fd16cd7ceade9') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:55:00' AND ends_at > '2026-10-07 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c04a05a465fb52fb812fd16cd7ceade9'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 06:40:00', '2026-10-07 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 16:20:00 +0700
-- source showtime_id: 6275f69a-ebf2-4cc8-b2eb-c4f25a1d18c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b4c539a994fa579a92c603bd1a0c9ece') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:35:00' AND ends_at > '2026-10-07 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b4c539a994fa579a92c603bd1a0c9ece'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 09:20:00', '2026-10-07 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 19:00:00 +0700
-- source showtime_id: da49250f-4efd-4ac2-88cb-6fce9b8b749c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('163ec481139a5fb5929385b512eff527') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:15:00' AND ends_at > '2026-10-07 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('163ec481139a5fb5929385b512eff527'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 12:00:00', '2026-10-07 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 03; local 2026-10-07 21:40:00 +0700
-- source showtime_id: c13c675f-a79f-493b-b294-98b19970dd11
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0d6a72da61915f0c9d2b5677e374cbc0') OR (room_id = @cinestar_seed_room_02 AND (starts_at = '2026-10-07 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:55:00' AND ends_at > '2026-10-07 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0d6a72da61915f0c9d2b5677e374cbc0'), @cinestar_seed_movie_15, @cinestar_seed_room_02, '2026-10-07 14:40:00', '2026-10-07 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_02 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-05 11:50:00 +0700
-- source showtime_id: 76143361-58f5-41cb-b25f-dd7c695c9d0b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d03e3be82e93589a88651a2abc9dd613') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-05 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:05:00' AND ends_at > '2026-10-05 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d03e3be82e93589a88651a2abc9dd613'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-05 04:50:00', '2026-10-05 07:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-05 14:30:00 +0700
-- source showtime_id: 52ebea89-6861-48a5-833b-c3b14de49454
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8ecfb76fb0f250f5a39bab930ab6e197') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-05 07:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:45:00' AND ends_at > '2026-10-05 07:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8ecfb76fb0f250f5a39bab930ab6e197'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-05 07:30:00', '2026-10-05 09:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-05 17:10:00 +0700
-- source showtime_id: 68ee117f-d5b2-42fc-b2e2-455503e6fbba
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0c9f6e8ced625d87a6abe87c3a466cec') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-05 10:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:25:00' AND ends_at > '2026-10-05 10:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0c9f6e8ced625d87a6abe87c3a466cec'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-05 10:10:00', '2026-10-05 12:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-05 19:50:00 +0700
-- source showtime_id: 273ab120-964f-48dd-9e4a-767a67b46903
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('18a58a32e62657669205a862986bdc5f') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-05 12:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:05:00' AND ends_at > '2026-10-05 12:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('18a58a32e62657669205a862986bdc5f'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-05 12:50:00', '2026-10-05 15:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-05 22:30:00 +0700
-- source showtime_id: dac0c38d-1c31-4c19-b89f-9648c2887b8a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('63f5d3aa22db5a72aa7d139f2aa9deae') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-05 15:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:45:00' AND ends_at > '2026-10-05 15:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('63f5d3aa22db5a72aa7d139f2aa9deae'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-05 15:30:00', '2026-10-05 17:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 09:10:00 +0700
-- source showtime_id: 191e873c-9824-4730-aac1-a6e72a065767
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f7d6cfdf9e425458bc3d80561f947c18') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 02:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:25:00' AND ends_at > '2026-10-06 02:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f7d6cfdf9e425458bc3d80561f947c18'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 02:10:00', '2026-10-06 04:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 11:50:00 +0700
-- source showtime_id: 79a00d9e-4cb5-4df5-aaf2-aaaad4783a93
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('db07c0f2dc0a5dff8f3449c3c0af8058') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:05:00' AND ends_at > '2026-10-06 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('db07c0f2dc0a5dff8f3449c3c0af8058'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 04:50:00', '2026-10-06 07:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 14:30:00 +0700
-- source showtime_id: 6ae26e3b-bbc3-48a0-a101-207784914919
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ccafa0f8fd135f198c3b212231b61c03') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 07:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:45:00' AND ends_at > '2026-10-06 07:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ccafa0f8fd135f198c3b212231b61c03'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 07:30:00', '2026-10-06 09:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 17:10:00 +0700
-- source showtime_id: 0def04af-de88-4c57-9496-41d8e75a937f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c55b7948527655929ae20337d4a53e3e') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 10:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:25:00' AND ends_at > '2026-10-06 10:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c55b7948527655929ae20337d4a53e3e'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 10:10:00', '2026-10-06 12:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 19:50:00 +0700
-- source showtime_id: d9e87340-4009-4910-a120-cb2680f62350
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('96f424ef77b35987af6e785a69c5a370') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 12:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:05:00' AND ends_at > '2026-10-06 12:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('96f424ef77b35987af6e785a69c5a370'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 12:50:00', '2026-10-06 15:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-06 22:30:00 +0700
-- source showtime_id: 6155f70b-d1f3-4362-b82a-bd509c580049
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('43d4506087f658d68a6d5c14c25e86aa') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-06 15:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:45:00' AND ends_at > '2026-10-06 15:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('43d4506087f658d68a6d5c14c25e86aa'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-06 15:30:00', '2026-10-06 17:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-07 09:00:00 +0700
-- source showtime_id: 33b3a2c8-05d1-4467-902e-9a539ecc113f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('18d572f519375670900eeb0b26c1b297') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-07 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:15:00' AND ends_at > '2026-10-07 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('18d572f519375670900eeb0b26c1b297'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-07 02:00:00', '2026-10-07 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-07 11:40:00 +0700
-- source showtime_id: 21f290a5-ca18-4690-aa91-fdaa291f2486
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('05ccd66d25f45025a85f5ed989b75ca2') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-07 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:55:00' AND ends_at > '2026-10-07 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('05ccd66d25f45025a85f5ed989b75ca2'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-07 04:40:00', '2026-10-07 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-07 14:20:00 +0700
-- source showtime_id: fe7e92c5-37a3-40be-8069-bd560d933ead
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5eb3005f1421594088ba9755e5ff2af6') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-07 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:35:00' AND ends_at > '2026-10-07 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5eb3005f1421594088ba9755e5ff2af6'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-07 07:20:00', '2026-10-07 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-07 17:15:00 +0700
-- source showtime_id: d28e42ec-84da-43bc-9fc7-e375d48f6ca4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3f709777269452a28b8b30ce790ba297') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-07 10:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:30:00' AND ends_at > '2026-10-07 10:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3f709777269452a28b8b30ce790ba297'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-07 10:15:00', '2026-10-07 12:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 02; local 2026-10-07 20:15:00 +0700
-- source showtime_id: a524f2c9-4b2f-4ab2-a2cb-92651ee1b5f5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('226c81de293f5396ab773ebf13b90092') OR (room_id = @cinestar_seed_room_03 AND (starts_at = '2026-10-07 13:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:30:00' AND ends_at > '2026-10-07 13:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('226c81de293f5396ab773ebf13b90092'), @cinestar_seed_movie_15, @cinestar_seed_room_03, '2026-10-07 13:15:00', '2026-10-07 15:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_03 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-05 11:00:00 +0700
-- source showtime_id: a9127253-9a55-4321-97c2-174951c2be1d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('622b827664bd50f9b41735ef7e73a37e') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-05 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:15:00' AND ends_at > '2026-10-05 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('622b827664bd50f9b41735ef7e73a37e'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-05 04:00:00', '2026-10-05 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-05 13:40:00 +0700
-- source showtime_id: 5b5bbcb1-00cd-4e14-852f-6f97edd16086
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ebf3679fbadb5561b3efb9f0b3d84811') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-05 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:55:00' AND ends_at > '2026-10-05 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ebf3679fbadb5561b3efb9f0b3d84811'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-05 06:40:00', '2026-10-05 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-05 16:20:00 +0700
-- source showtime_id: cfddd585-863f-49f3-814f-bb53944dbac8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8d202e08bd165c279b5689c7401a4627') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:35:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8d202e08bd165c279b5689c7401a4627'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-05 09:20:00', '2026-10-05 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-05 19:00:00 +0700
-- source showtime_id: ba56b65c-f417-4071-a52a-092a4c40ae62
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('94e5aa0b47e45b37b715efb80e5bf877') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-05 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:15:00' AND ends_at > '2026-10-05 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('94e5aa0b47e45b37b715efb80e5bf877'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-05 12:00:00', '2026-10-05 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-05 21:40:00 +0700
-- source showtime_id: 6ac263f5-ab7f-44ba-8e96-6f7d2f077ca5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('78919d14ac205db499f6c74fdfc56a72') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-05 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:55:00' AND ends_at > '2026-10-05 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('78919d14ac205db499f6c74fdfc56a72'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-05 14:40:00', '2026-10-05 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 08:20:00 +0700
-- source showtime_id: 971f2b60-5835-4137-bcdd-dd192608d349
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('28e604f0709258fda6613b0ac9ed9806') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 01:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:35:00' AND ends_at > '2026-10-06 01:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('28e604f0709258fda6613b0ac9ed9806'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 01:20:00', '2026-10-06 03:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 11:00:00 +0700
-- source showtime_id: 4a119ad0-d456-4efe-8a7e-e8151f87fa46
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a3cabb1e9624552db3d6883cfa043a8c') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:15:00' AND ends_at > '2026-10-06 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a3cabb1e9624552db3d6883cfa043a8c'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 04:00:00', '2026-10-06 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 13:40:00 +0700
-- source showtime_id: 8cdf1f13-19eb-4108-a163-90e1e392250b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e55dc091e154510cb2f674ae99656e20') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:55:00' AND ends_at > '2026-10-06 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e55dc091e154510cb2f674ae99656e20'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 06:40:00', '2026-10-06 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 16:20:00 +0700
-- source showtime_id: 8a4a46fb-675c-49c6-8133-614f69198fc7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('05e339a0cae95ed890764793c4c3481d') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:35:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('05e339a0cae95ed890764793c4c3481d'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 09:20:00', '2026-10-06 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 19:00:00 +0700
-- source showtime_id: 252256f7-7291-4176-a103-bfe5b5d5be7f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d036b4895af85b7786a17ec206e01980') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:15:00' AND ends_at > '2026-10-06 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d036b4895af85b7786a17ec206e01980'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 12:00:00', '2026-10-06 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-06 21:40:00 +0700
-- source showtime_id: fcdef0cf-3c83-4135-b73c-516b19810448
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c1b21f23fc055e788aaa6650126b2179') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-06 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:55:00' AND ends_at > '2026-10-06 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c1b21f23fc055e788aaa6650126b2179'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-06 14:40:00', '2026-10-06 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-07 09:30:00 +0700
-- source showtime_id: 4d7405e4-3b4f-4120-9c57-d0a121717e23
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('187364e622885f1a8d5d4a5c9e8d292a') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-07 02:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:45:00' AND ends_at > '2026-10-07 02:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('187364e622885f1a8d5d4a5c9e8d292a'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-07 02:30:00', '2026-10-07 04:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-07 12:10:00 +0700
-- source showtime_id: 41ee871b-6da6-4985-9824-1401c173c59e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('82206f5fc2df537d880542774a484eb7') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-07 05:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:25:00' AND ends_at > '2026-10-07 05:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('82206f5fc2df537d880542774a484eb7'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-07 05:10:00', '2026-10-07 07:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-07 14:50:00 +0700
-- source showtime_id: c8cc322c-c183-4bce-8c47-209410d9be40
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03dde7ae1c3f5be9a0826d0c515c71fa') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-07 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:05:00' AND ends_at > '2026-10-07 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03dde7ae1c3f5be9a0826d0c515c71fa'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-07 07:50:00', '2026-10-07 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-07 17:35:00 +0700
-- source showtime_id: 08b07bb7-ef82-4903-b798-292de7d992d3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1d2ae0e80b0a5bcf89a6e609821a4425') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-07 10:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:50:00' AND ends_at > '2026-10-07 10:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1d2ae0e80b0a5bcf89a6e609821a4425'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-07 10:35:00', '2026-10-07 12:50:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Lâm Đồng (Đức Trọng); room 01; local 2026-10-07 20:45:00 +0700
-- source showtime_id: 4e62dd9c-092e-43f9-b065-e3c4fbc1aad4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('41965b7b9a6d518fa7c13debe35e2391') OR (room_id = @cinestar_seed_room_04 AND (starts_at = '2026-10-07 13:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:00:00' AND ends_at > '2026-10-07 13:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('41965b7b9a6d518fa7c13debe35e2391'), @cinestar_seed_movie_15, @cinestar_seed_room_04, '2026-10-07 13:45:00', '2026-10-07 16:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_04 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-05 11:40:00 +0700
-- source showtime_id: e7011111-08d3-43be-9522-cbf250d6e33b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8264d9881c3854a2a887c48417633523') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-05 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:55:00' AND ends_at > '2026-10-05 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8264d9881c3854a2a887c48417633523'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-05 04:40:00', '2026-10-05 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-05 14:20:00 +0700
-- source showtime_id: 15539c46-f567-432c-b29a-858a75b78f0a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('258f4250c432590e9075f8ffd4875364') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-05 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:35:00' AND ends_at > '2026-10-05 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('258f4250c432590e9075f8ffd4875364'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-05 07:20:00', '2026-10-05 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-05 17:00:00 +0700
-- source showtime_id: a636cd56-da32-4d41-a827-cc83633c1007
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cbf0cc940d7c5eaf9093c679e635a1fd') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-05 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:15:00' AND ends_at > '2026-10-05 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cbf0cc940d7c5eaf9093c679e635a1fd'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-05 10:00:00', '2026-10-05 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-05 19:40:00 +0700
-- source showtime_id: a9377f52-b559-4ddb-9f6d-d9c74e5708ab
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5a3dd83d6706547789a389e8ebc576aa') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-05 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:55:00' AND ends_at > '2026-10-05 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5a3dd83d6706547789a389e8ebc576aa'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-05 12:40:00', '2026-10-05 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-05 22:20:00 +0700
-- source showtime_id: bfee098d-74f9-4055-bdf4-2e6b8bad1b59
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8cea99060eb85d3fa42428a6cb038c60') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-05 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:35:00' AND ends_at > '2026-10-05 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8cea99060eb85d3fa42428a6cb038c60'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-05 15:20:00', '2026-10-05 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 09:00:00 +0700
-- source showtime_id: ab37539b-f709-4666-aa9a-bd3922b859c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c7eafba71b8550c8b0b8d62830ba5ee8') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:15:00' AND ends_at > '2026-10-06 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c7eafba71b8550c8b0b8d62830ba5ee8'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 02:00:00', '2026-10-06 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 11:40:00 +0700
-- source showtime_id: a3941b37-2d36-4266-a1bb-ac127846d7f6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4fef043c3a29584cbcd67f513091ae5e') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:55:00' AND ends_at > '2026-10-06 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4fef043c3a29584cbcd67f513091ae5e'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 04:40:00', '2026-10-06 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 14:20:00 +0700
-- source showtime_id: c400cb21-c303-42fc-bd6e-e1757c74f401
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b87bb28206185e87bde0397a83441e90') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:35:00' AND ends_at > '2026-10-06 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b87bb28206185e87bde0397a83441e90'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 07:20:00', '2026-10-06 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 17:00:00 +0700
-- source showtime_id: dd4bb5fc-2dcd-4aa6-b0cc-15c55e0ea5ac
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4089b9759c1c5d2f8f6d9bb3a2f8c980') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:15:00' AND ends_at > '2026-10-06 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4089b9759c1c5d2f8f6d9bb3a2f8c980'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 10:00:00', '2026-10-06 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 19:40:00 +0700
-- source showtime_id: a8fcfb87-9cc6-4d12-9970-3d5cbe1bb910
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c25eec8db40c5e5f986a3a64da96bccf') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:55:00' AND ends_at > '2026-10-06 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c25eec8db40c5e5f986a3a64da96bccf'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 12:40:00', '2026-10-06 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-06 22:20:00 +0700
-- source showtime_id: a1183046-3ab6-48f1-a72f-64b8dc7fb634
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('812c5482c6f75dcc84eb2b5a0929cba1') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-06 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:35:00' AND ends_at > '2026-10-06 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('812c5482c6f75dcc84eb2b5a0929cba1'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-06 15:20:00', '2026-10-06 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 09:00:00 +0700
-- source showtime_id: dc95b5c9-68f5-4ffd-88e7-1ae812f84493
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('80c605727d205d408e0182e707c726e9') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:15:00' AND ends_at > '2026-10-07 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('80c605727d205d408e0182e707c726e9'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 02:00:00', '2026-10-07 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 11:40:00 +0700
-- source showtime_id: f04463c4-d33f-47eb-8e1f-6bf56b159769
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3830af1e8084573196e8ab877292dc30') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:55:00' AND ends_at > '2026-10-07 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3830af1e8084573196e8ab877292dc30'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 04:40:00', '2026-10-07 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 14:20:00 +0700
-- source showtime_id: 603139d2-41eb-49d0-ba1a-bafa7fad5a23
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('63fa341117d75ebf9bdb1028da649af2') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:35:00' AND ends_at > '2026-10-07 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('63fa341117d75ebf9bdb1028da649af2'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 07:20:00', '2026-10-07 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 17:00:00 +0700
-- source showtime_id: 8dbc6757-da92-4bb1-a103-71c87c7460d5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f7703324a2d05892b80013f16efa7245') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:15:00' AND ends_at > '2026-10-07 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f7703324a2d05892b80013f16efa7245'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 10:00:00', '2026-10-07 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 19:40:00 +0700
-- source showtime_id: 836bda53-3bc6-48e9-b45f-df853ce6b420
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('407598a7b6b05f6b916024ea5a4b9b44') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:55:00' AND ends_at > '2026-10-07 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('407598a7b6b05f6b916024ea5a4b9b44'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 12:40:00', '2026-10-07 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 01; local 2026-10-07 22:20:00 +0700
-- source showtime_id: b3ded131-d015-4e15-a6ca-5222bb3e3984
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3f5c6d5ec6fe5209ac874065c0e1cb86') OR (room_id = @cinestar_seed_room_05 AND (starts_at = '2026-10-07 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 17:35:00' AND ends_at > '2026-10-07 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3f5c6d5ec6fe5209ac874065c0e1cb86'), @cinestar_seed_movie_15, @cinestar_seed_room_05, '2026-10-07 15:20:00', '2026-10-07 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_05 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 11:00:00 +0700
-- source showtime_id: 7c782c34-1a97-482c-9e90-9ca661183e2c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cbbaa74b2aa25f94bbc6d0cad006130a') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:15:00' AND ends_at > '2026-10-05 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cbbaa74b2aa25f94bbc6d0cad006130a'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 04:00:00', '2026-10-05 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 13:40:00 +0700
-- source showtime_id: bec80167-8e0a-4174-a8ee-f43fd46ee705
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1517a829891a572b96600aacf23db87b') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:55:00' AND ends_at > '2026-10-05 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1517a829891a572b96600aacf23db87b'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 06:40:00', '2026-10-05 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 16:20:00 +0700
-- source showtime_id: 13c8ff74-65ac-4d7c-84dc-5011c02f2ae2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4f777d52874c5699b6ed3df6e35ce7e0') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:35:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4f777d52874c5699b6ed3df6e35ce7e0'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 09:20:00', '2026-10-05 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 19:00:00 +0700
-- source showtime_id: e5bc5bed-76a1-409b-8dd6-356cd3b9a7b5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9fc5ea15cb6d5f01becda6fa1d92bee3') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:15:00' AND ends_at > '2026-10-05 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9fc5ea15cb6d5f01becda6fa1d92bee3'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 12:00:00', '2026-10-05 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 21:40:00 +0700
-- source showtime_id: 4c2fdd44-44ea-4504-9dd0-716b787d661f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2b5c4b5d984a5ff698493a241c8b85d0') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:55:00' AND ends_at > '2026-10-05 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2b5c4b5d984a5ff698493a241c8b85d0'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 14:40:00', '2026-10-05 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-05 23:59:00 +0700
-- source showtime_id: 70620e0d-5d58-4734-9c76-c11fedbce4b0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('62f3755c205c5e34a70d348ad12064d4') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-05 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 19:14:00' AND ends_at > '2026-10-05 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('62f3755c205c5e34a70d348ad12064d4'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-05 16:59:00', '2026-10-05 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 11:00:00 +0700
-- source showtime_id: f7d59a12-b3a3-460a-91cd-42a0f88b82e1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0917df5b7ac25b8b8e09981bbb901e65') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:15:00' AND ends_at > '2026-10-06 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0917df5b7ac25b8b8e09981bbb901e65'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 04:00:00', '2026-10-06 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 13:40:00 +0700
-- source showtime_id: 1d797e20-6c8e-45c2-ba95-8cd480557795
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a1686fbbe9125e559b84fbbe11db535e') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:55:00' AND ends_at > '2026-10-06 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a1686fbbe9125e559b84fbbe11db535e'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 06:40:00', '2026-10-06 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 16:20:00 +0700
-- source showtime_id: 7ad58f54-5f85-4c55-ad23-6ace84cca17b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8e262bc0c32d579892ac04bb6cc96692') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:35:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8e262bc0c32d579892ac04bb6cc96692'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 09:20:00', '2026-10-06 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 19:00:00 +0700
-- source showtime_id: 10689d1f-43c4-47ea-b195-999abbecc8cd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('453c8df219165c39abb0e3fed44166ab') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:15:00' AND ends_at > '2026-10-06 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('453c8df219165c39abb0e3fed44166ab'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 12:00:00', '2026-10-06 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 21:40:00 +0700
-- source showtime_id: ac7092da-9b71-405e-9073-da6c34b5c8ea
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e84b2f05050d5d91b811266301ea55b9') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:55:00' AND ends_at > '2026-10-06 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e84b2f05050d5d91b811266301ea55b9'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 14:40:00', '2026-10-06 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-06 23:59:00 +0700
-- source showtime_id: d176cf0d-26ea-4963-872e-aa098fb4b1fd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('daa2e210c975572494adf7fa4407fad3') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-06 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 19:14:00' AND ends_at > '2026-10-06 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('daa2e210c975572494adf7fa4407fad3'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-06 16:59:00', '2026-10-06 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 08:20:00 +0700
-- source showtime_id: 9d95eeaf-be9b-45fe-9e7e-827b06589a11
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9c9048f56cae56c7807d2b75b49e022e') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 01:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:35:00' AND ends_at > '2026-10-07 01:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9c9048f56cae56c7807d2b75b49e022e'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 01:20:00', '2026-10-07 03:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 11:00:00 +0700
-- source showtime_id: 462d8059-b551-4f79-9fba-898f60a02f87
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d96b802d2ffe567e98660f1ddeee68e8') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:15:00' AND ends_at > '2026-10-07 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d96b802d2ffe567e98660f1ddeee68e8'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 04:00:00', '2026-10-07 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 13:40:00 +0700
-- source showtime_id: 1c7b8b18-9410-4419-81c5-fe4dd581c9d0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2b3a5c6b9c895fc98ef61ac03b7d1195') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:55:00' AND ends_at > '2026-10-07 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2b3a5c6b9c895fc98ef61ac03b7d1195'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 06:40:00', '2026-10-07 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 16:20:00 +0700
-- source showtime_id: a7600129-f889-4507-8c28-c54a5d406d0a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b8622fd97fb1502c8a9ab6a3a3eb35a7') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:35:00' AND ends_at > '2026-10-07 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b8622fd97fb1502c8a9ab6a3a3eb35a7'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 09:20:00', '2026-10-07 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 19:00:00 +0700
-- source showtime_id: 1eef3ad8-2c87-4745-9835-5c2c385a2843
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7e8a9ce5da855495852bdfaeaa2c47df') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:15:00' AND ends_at > '2026-10-07 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7e8a9ce5da855495852bdfaeaa2c47df'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 12:00:00', '2026-10-07 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 21:40:00 +0700
-- source showtime_id: 88449723-15a1-4fb1-bec3-2901dc155c01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d118b5c9777a5463a8c97a937c4df017') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:55:00' AND ends_at > '2026-10-07 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d118b5c9777a5463a8c97a937c4df017'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 14:40:00', '2026-10-07 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 02; local 2026-10-07 23:59:00 +0700
-- source showtime_id: cbd5da73-0772-4f18-b719-350e5a9556d9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2d3557a9ac1158d4a27c92f476d220b6') OR (room_id = @cinestar_seed_room_06 AND (starts_at = '2026-10-07 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 19:14:00' AND ends_at > '2026-10-07 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2d3557a9ac1158d4a27c92f476d220b6'), @cinestar_seed_movie_15, @cinestar_seed_room_06, '2026-10-07 16:59:00', '2026-10-07 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_06 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-05 13:00:00 +0700
-- source showtime_id: 0dba05c4-cbae-4417-9ad3-11c599750f78
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f07d1352668653f39e22cd6f2c3c7524') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-05 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:15:00' AND ends_at > '2026-10-05 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f07d1352668653f39e22cd6f2c3c7524'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-05 06:00:00', '2026-10-05 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-05 15:40:00 +0700
-- source showtime_id: 1de7beb0-cfb7-4230-841d-6354587f1f49
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e0605e46d4f45131ae6676a275900c99') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-05 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:55:00' AND ends_at > '2026-10-05 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e0605e46d4f45131ae6676a275900c99'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-05 08:40:00', '2026-10-05 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-05 18:20:00 +0700
-- source showtime_id: 479d60bc-109f-4b6e-b3a6-f405d29f6358
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b2339e2074d75edabe70dc9af4a675fa') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-05 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:35:00' AND ends_at > '2026-10-05 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b2339e2074d75edabe70dc9af4a675fa'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-05 11:20:00', '2026-10-05 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-05 21:00:00 +0700
-- source showtime_id: 787663bd-7303-43dd-b9d6-bfa36cbd9bd8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('33649466c1ea559ab31d5dfe81e5d43c') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-05 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:15:00' AND ends_at > '2026-10-05 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('33649466c1ea559ab31d5dfe81e5d43c'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-05 14:00:00', '2026-10-05 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-05 23:40:00 +0700
-- source showtime_id: 5a54356d-a7fc-4fd9-a7aa-a0ea1dfeb433
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ecb6380ad0145559b8ef11cb8ee95a0d') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-05 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:55:00' AND ends_at > '2026-10-05 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ecb6380ad0145559b8ef11cb8ee95a0d'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-05 16:40:00', '2026-10-05 18:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 08:10:00 +0700
-- source showtime_id: d66c758e-6253-4a8a-b951-f62c2e6b7065
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2e19be747aa45517bc68d694a998e995') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 01:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:51:00' AND ends_at > '2026-10-06 01:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2e19be747aa45517bc68d694a998e995'), @cinestar_seed_movie_29, @cinestar_seed_room_07, '2026-10-06 01:10:00', '2026-10-06 02:51:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 10:20:00 +0700
-- source showtime_id: 0b5a3b90-b1fa-4595-a791-be5562c58064
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8552d37957865445aba9a474f84582f0') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 03:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:35:00' AND ends_at > '2026-10-06 03:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8552d37957865445aba9a474f84582f0'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 03:20:00', '2026-10-06 05:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 13:00:00 +0700
-- source showtime_id: 2b644020-88e4-4c2e-8389-b5844b55f10d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a34a9b8d197c5328bb958ca62aecb315') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:15:00' AND ends_at > '2026-10-06 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a34a9b8d197c5328bb958ca62aecb315'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 06:00:00', '2026-10-06 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 15:40:00 +0700
-- source showtime_id: 2c2751a1-7133-4db5-80cc-68584e7eb1bc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d9825e800a21582ebd092a165c41e537') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:55:00' AND ends_at > '2026-10-06 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d9825e800a21582ebd092a165c41e537'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 08:40:00', '2026-10-06 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 18:20:00 +0700
-- source showtime_id: aa9496e3-9cd6-408b-bce4-8ead28567708
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('20e05c2e3d55551d80eb2c9a549b8088') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:35:00' AND ends_at > '2026-10-06 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('20e05c2e3d55551d80eb2c9a549b8088'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 11:20:00', '2026-10-06 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 21:00:00 +0700
-- source showtime_id: e260e8a8-c871-4b4d-8aea-d2237f9f300c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6bc9da41c5f65d30808dbb9e9a965c62') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:15:00' AND ends_at > '2026-10-06 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6bc9da41c5f65d30808dbb9e9a965c62'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 14:00:00', '2026-10-06 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 03; local 2026-10-06 23:40:00 +0700
-- source showtime_id: 6ac9a718-7132-4e34-b8ae-8e4841072e35
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9e44e3addacf5588bebf4930bad12239') OR (room_id = @cinestar_seed_room_07 AND (starts_at = '2026-10-06 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:55:00' AND ends_at > '2026-10-06 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9e44e3addacf5588bebf4930bad12239'), @cinestar_seed_movie_15, @cinestar_seed_room_07, '2026-10-06 16:40:00', '2026-10-06 18:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_07 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 11:50:00 +0700
-- source showtime_id: 9b4c55b6-eda3-4684-93ef-b92125dcb69a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a69baf6e80515afd93e54ff5240a98db') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:26:00' AND ends_at > '2026-10-05 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a69baf6e80515afd93e54ff5240a98db'), @cinestar_seed_movie_06, @cinestar_seed_room_08, '2026-10-05 04:50:00', '2026-10-05 06:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 13:50:00 +0700
-- source showtime_id: 6e64171b-ca42-416b-bf29-c93bee611a07
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8d4bcde1a8ae574d863d5f679f454a1a') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:45:00' AND ends_at > '2026-10-05 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8d4bcde1a8ae574d863d5f679f454a1a'), @cinestar_seed_movie_07, @cinestar_seed_room_08, '2026-10-05 06:50:00', '2026-10-05 08:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 16:10:00 +0700
-- source showtime_id: 1a78d92e-e40b-4b45-9bc6-3b7de6e4e878
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a5173d0eaffa566a80f6a97300ca6357') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 09:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:30:00' AND ends_at > '2026-10-05 09:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a5173d0eaffa566a80f6a97300ca6357'), @cinestar_seed_movie_08, @cinestar_seed_room_08, '2026-10-05 09:10:00', '2026-10-05 10:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 18:00:00 +0700
-- source showtime_id: 577ac572-af57-463d-b27c-66d48c3ecbec
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bab271d6a1b55d19bb0424b7c850a130') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:15:00' AND ends_at > '2026-10-05 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bab271d6a1b55d19bb0424b7c850a130'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-05 11:00:00', '2026-10-05 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 20:40:00 +0700
-- source showtime_id: 2e118b57-10db-4844-9b53-48c2b068fbd1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5196974ca934550a98f10f335a817e18') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:55:00' AND ends_at > '2026-10-05 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5196974ca934550a98f10f335a817e18'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-05 13:40:00', '2026-10-05 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-05 23:20:00 +0700
-- source showtime_id: 885ce29c-f862-45f9-b544-e0d8cffd6346
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e1d61f806a325fe2b4aaee5e553c3ab8') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-05 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:35:00' AND ends_at > '2026-10-05 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e1d61f806a325fe2b4aaee5e553c3ab8'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-05 16:20:00', '2026-10-05 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 19d4c498-d03c-4ab5-839f-cdb1b6da62cd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('60b09d4cc459540485de1687e40369f1') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:20:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('60b09d4cc459540485de1687e40369f1'), @cinestar_seed_movie_08, @cinestar_seed_room_08, '2026-10-06 01:00:00', '2026-10-06 02:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 09:40:00 +0700
-- source showtime_id: 80fc2c5c-1fe7-4cfa-90b3-0bd0357176a9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a80126316b355b56948c3e31cecfbb7e') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 02:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:27:00' AND ends_at > '2026-10-06 02:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a80126316b355b56948c3e31cecfbb7e'), @cinestar_seed_movie_13, @cinestar_seed_room_08, '2026-10-06 02:40:00', '2026-10-06 04:27:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 11:50:00 +0700
-- source showtime_id: 3923a5f1-c044-40e0-ad26-12c82c6d2fb8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1f4f136734725c528f50d8423c7ad6a9') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:26:00' AND ends_at > '2026-10-06 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1f4f136734725c528f50d8423c7ad6a9'), @cinestar_seed_movie_06, @cinestar_seed_room_08, '2026-10-06 04:50:00', '2026-10-06 06:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 13:50:00 +0700
-- source showtime_id: 7bf99147-250e-4686-b74d-4f3a5925ba83
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2613ca77c4c85cc1a1b050478705e90c') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:45:00' AND ends_at > '2026-10-06 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2613ca77c4c85cc1a1b050478705e90c'), @cinestar_seed_movie_07, @cinestar_seed_room_08, '2026-10-06 06:50:00', '2026-10-06 08:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 16:10:00 +0700
-- source showtime_id: 5ad9dda2-15cd-4195-b722-0475498eeced
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b8f4e56f2e725e9989a50e50fd161258') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 09:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:30:00' AND ends_at > '2026-10-06 09:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b8f4e56f2e725e9989a50e50fd161258'), @cinestar_seed_movie_08, @cinestar_seed_room_08, '2026-10-06 09:10:00', '2026-10-06 10:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 18:00:00 +0700
-- source showtime_id: cd883ff3-551f-4532-9519-ce575d4e4544
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3d0dc8ea11255dbd809d48f55a3b249c') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:15:00' AND ends_at > '2026-10-06 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3d0dc8ea11255dbd809d48f55a3b249c'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-06 11:00:00', '2026-10-06 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 20:40:00 +0700
-- source showtime_id: 7cd5da67-269a-4f85-b792-d66f5de38803
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('533f9792c33453c08c264a805c278985') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:55:00' AND ends_at > '2026-10-06 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('533f9792c33453c08c264a805c278985'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-06 13:40:00', '2026-10-06 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 05; local 2026-10-06 23:20:00 +0700
-- source showtime_id: 177ef946-858d-40f7-ba90-ae7f437e9c4c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('30237f91ba2c5a2c9a238a4b5ec1b108') OR (room_id = @cinestar_seed_room_08 AND (starts_at = '2026-10-06 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:35:00' AND ends_at > '2026-10-06 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('30237f91ba2c5a2c9a238a4b5ec1b108'), @cinestar_seed_movie_15, @cinestar_seed_room_08, '2026-10-06 16:20:00', '2026-10-06 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_08 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-05 12:20:00 +0700
-- source showtime_id: 520257ba-a57a-4ea6-9fc9-9dae0306518c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('825dfceee3815984ad648919eca635bf') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-05 05:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:35:00' AND ends_at > '2026-10-05 05:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('825dfceee3815984ad648919eca635bf'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-05 05:20:00', '2026-10-05 07:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-05 15:00:00 +0700
-- source showtime_id: 2588be02-4a39-4a99-abd6-827f2f6ecfec
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fd71a63058b25013b111fe3febed0c9b') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-05 08:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:15:00' AND ends_at > '2026-10-05 08:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fd71a63058b25013b111fe3febed0c9b'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-05 08:00:00', '2026-10-05 10:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-05 17:40:00 +0700
-- source showtime_id: 27114273-b0b0-448e-bd69-b3af6ad62941
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('eaf176ec660e5fb69567d8a25c403c6e') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-05 10:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:55:00' AND ends_at > '2026-10-05 10:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('eaf176ec660e5fb69567d8a25c403c6e'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-05 10:40:00', '2026-10-05 12:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-05 20:20:00 +0700
-- source showtime_id: 3081d0c1-b3a9-4b31-bb9b-0b5c23fbe711
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('58482ff16c3d5cc5928864cbf7aa336c') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-05 13:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:35:00' AND ends_at > '2026-10-05 13:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('58482ff16c3d5cc5928864cbf7aa336c'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-05 13:20:00', '2026-10-05 15:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-05 23:00:00 +0700
-- source showtime_id: 2870acc6-20d1-4cce-b064-b94981091e31
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2a907900463659cab48407686aaf29db') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-05 16:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:15:00' AND ends_at > '2026-10-05 16:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2a907900463659cab48407686aaf29db'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-05 16:00:00', '2026-10-05 18:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 443ff7cd-59d9-49d9-9e33-41e9683dbc89
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('22549cdac7bf5a6db67346121eb99e69') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:16:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('22549cdac7bf5a6db67346121eb99e69'), @cinestar_seed_movie_02, @cinestar_seed_room_09, '2026-10-06 01:00:00', '2026-10-06 02:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 09:40:00 +0700
-- source showtime_id: 144a5c6a-031e-4391-b8f2-c704a8ae449e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bb1abbe41482508e9de95b9a23e3bb90') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 02:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:55:00' AND ends_at > '2026-10-06 02:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bb1abbe41482508e9de95b9a23e3bb90'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 02:40:00', '2026-10-06 04:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 12:20:00 +0700
-- source showtime_id: abdc527e-0efc-4d90-9404-48103b4d7112
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5938ee82f5555348b22c4f1b93fefc04') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 05:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:35:00' AND ends_at > '2026-10-06 05:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5938ee82f5555348b22c4f1b93fefc04'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 05:20:00', '2026-10-06 07:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 15:00:00 +0700
-- source showtime_id: c2d24ecd-8d2a-40bb-8a35-ec7960695418
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('221541fa7c635b948ad5f5b3e727bef6') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 08:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:15:00' AND ends_at > '2026-10-06 08:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('221541fa7c635b948ad5f5b3e727bef6'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 08:00:00', '2026-10-06 10:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 17:40:00 +0700
-- source showtime_id: 5d00bb8b-f646-4653-95d5-2a0226e13a43
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e8808b8af5b25572a689afdf1e58c916') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 10:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:55:00' AND ends_at > '2026-10-06 10:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e8808b8af5b25572a689afdf1e58c916'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 10:40:00', '2026-10-06 12:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 20:20:00 +0700
-- source showtime_id: f755d28a-0fd4-46ae-bfb9-1f1a0828759d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('100598b167015b489a1819ae1fadba8c') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 13:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:35:00' AND ends_at > '2026-10-06 13:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('100598b167015b489a1819ae1fadba8c'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 13:20:00', '2026-10-06 15:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-06 23:00:00 +0700
-- source showtime_id: 36da98e0-bc0a-4337-8c08-383826542d35
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('52dce18f6f805cd09e6a766473ef668c') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-06 16:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:15:00' AND ends_at > '2026-10-06 16:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('52dce18f6f805cd09e6a766473ef668c'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-06 16:00:00', '2026-10-06 18:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 08:00:00 +0700
-- source showtime_id: d0d3d14f-a3d4-4260-8f99-44fbadc4be40
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b50484be92655595a0c8fbc9f3604de6') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 02:16:00' AND ends_at > '2026-10-07 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b50484be92655595a0c8fbc9f3604de6'), @cinestar_seed_movie_02, @cinestar_seed_room_09, '2026-10-07 01:00:00', '2026-10-07 02:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 09:40:00 +0700
-- source showtime_id: c5eb6dd2-c258-48b5-88c0-46e428c66c47
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('945bcc0b2c6a5275bdc2e5aaa7148d8f') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 02:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:55:00' AND ends_at > '2026-10-07 02:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('945bcc0b2c6a5275bdc2e5aaa7148d8f'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 02:40:00', '2026-10-07 04:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 12:20:00 +0700
-- source showtime_id: 243341e1-e72d-4de7-b159-891d85d7d0a2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('758101b643e05d03a92c957e47113784') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 05:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:35:00' AND ends_at > '2026-10-07 05:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('758101b643e05d03a92c957e47113784'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 05:20:00', '2026-10-07 07:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 15:00:00 +0700
-- source showtime_id: a84c7778-7528-48c3-935b-fabcdd966679
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d72d1964838d5b22b4f470971f379b4c') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 08:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:15:00' AND ends_at > '2026-10-07 08:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d72d1964838d5b22b4f470971f379b4c'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 08:00:00', '2026-10-07 10:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 17:40:00 +0700
-- source showtime_id: 0620e02a-a26e-4d20-8bb8-380ba027a150
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a27991255f7e58bd8c635144bc14ac75') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 10:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:55:00' AND ends_at > '2026-10-07 10:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a27991255f7e58bd8c635144bc14ac75'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 10:40:00', '2026-10-07 12:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 20:20:00 +0700
-- source showtime_id: 53a38068-6bfe-481d-81a2-6b09971661cb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8de4fc5dc6375dc19dcfa54348cb81cd') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 13:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:35:00' AND ends_at > '2026-10-07 13:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8de4fc5dc6375dc19dcfa54348cb81cd'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 13:20:00', '2026-10-07 15:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Satra Quận 6 (TP.HCM); room 07; local 2026-10-07 23:00:00 +0700
-- source showtime_id: 75cfa254-1474-440a-8f54-7c050f6d77a2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('296ba5965dd8530ca4fab712653617c8') OR (room_id = @cinestar_seed_room_09 AND (starts_at = '2026-10-07 16:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 18:15:00' AND ends_at > '2026-10-07 16:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('296ba5965dd8530ca4fab712653617c8'), @cinestar_seed_movie_15, @cinestar_seed_room_09, '2026-10-07 16:00:00', '2026-10-07 18:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_09 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 01; local 2026-10-05 13:20:00 +0700
-- source showtime_id: 02a9a90a-fabc-47fe-8dcb-fd3525eaf8c9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a64b9d016abe5bbca1455b00a652ed4f') OR (room_id = @cinestar_seed_room_10 AND (starts_at = '2026-10-05 06:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:35:00' AND ends_at > '2026-10-05 06:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a64b9d016abe5bbca1455b00a652ed4f'), @cinestar_seed_movie_15, @cinestar_seed_room_10, '2026-10-05 06:20:00', '2026-10-05 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_10 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 01; local 2026-10-05 16:00:00 +0700
-- source showtime_id: 40cf0554-768e-4b22-8a8a-ea2860cb8d50
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a4552377f5d0577b9216292865c198f0') OR (room_id = @cinestar_seed_room_10 AND (starts_at = '2026-10-05 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:15:00' AND ends_at > '2026-10-05 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a4552377f5d0577b9216292865c198f0'), @cinestar_seed_movie_15, @cinestar_seed_room_10, '2026-10-05 09:00:00', '2026-10-05 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_10 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 01; local 2026-10-05 18:40:00 +0700
-- source showtime_id: 7852c38b-0e3a-4c4b-bc9a-039b96354f01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6b13769f727e5bd5a8167f679c52c2df') OR (room_id = @cinestar_seed_room_10 AND (starts_at = '2026-10-05 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:55:00' AND ends_at > '2026-10-05 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6b13769f727e5bd5a8167f679c52c2df'), @cinestar_seed_movie_15, @cinestar_seed_room_10, '2026-10-05 11:40:00', '2026-10-05 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_10 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 01; local 2026-10-05 21:20:00 +0700
-- source showtime_id: 7708b723-89b4-4b09-a52a-496270d5a328
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e9cdaadc33b85b03a06bef6c277ccb47') OR (room_id = @cinestar_seed_room_10 AND (starts_at = '2026-10-05 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:35:00' AND ends_at > '2026-10-05 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e9cdaadc33b85b03a06bef6c277ccb47'), @cinestar_seed_movie_15, @cinestar_seed_room_10, '2026-10-05 14:20:00', '2026-10-05 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_10 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-05 12:50:00 +0700
-- source showtime_id: afa50c4e-405e-406d-949c-6693f5e82520
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5bd9fcbfae625578855a517ae64b9ba0') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-05 05:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:37:00' AND ends_at > '2026-10-05 05:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5bd9fcbfae625578855a517ae64b9ba0'), @cinestar_seed_movie_13, @cinestar_seed_room_11, '2026-10-05 05:50:00', '2026-10-05 07:37:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-05 15:00:00 +0700
-- source showtime_id: 1b7cd2b3-6443-4cfe-b5f0-192ad52332f6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('dcd9c1f6ce2958a6ba46099e1ce6b5a1') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-05 08:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:55:00' AND ends_at > '2026-10-05 08:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('dcd9c1f6ce2958a6ba46099e1ce6b5a1'), @cinestar_seed_movie_04, @cinestar_seed_room_11, '2026-10-05 08:00:00', '2026-10-05 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-05 17:20:00 +0700
-- source showtime_id: f459fac7-b775-484d-9ca1-c02e43a9a583
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b11e80c088955b65b72b298fc0d910a5') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-05 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:35:00' AND ends_at > '2026-10-05 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b11e80c088955b65b72b298fc0d910a5'), @cinestar_seed_movie_15, @cinestar_seed_room_11, '2026-10-05 10:20:00', '2026-10-05 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-05 20:00:00 +0700
-- source showtime_id: 5afeddb8-bc74-4176-bf19-d03fede0983b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('52c70be087ef57b18902646facfe4fff') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-05 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:15:00' AND ends_at > '2026-10-05 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('52c70be087ef57b18902646facfe4fff'), @cinestar_seed_movie_15, @cinestar_seed_room_11, '2026-10-05 13:00:00', '2026-10-05 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-05 22:40:00 +0700
-- source showtime_id: 4b834491-a523-46dc-b23b-37922bb80be7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d6fae6eca5135d4e860d77c1a29f5d78') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:55:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d6fae6eca5135d4e860d77c1a29f5d78'), @cinestar_seed_movie_15, @cinestar_seed_room_11, '2026-10-05 15:40:00', '2026-10-05 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-14 19:00:00 +0700
-- source showtime_id: c74148be-b8ea-4df8-b59a-a42b2911d71e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8f9ee071389a5228b6dfe3b4fdb2c937') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-14 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-14 13:38:00' AND ends_at > '2026-10-14 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8f9ee071389a5228b6dfe3b4fdb2c937'), @cinestar_seed_movie_12, @cinestar_seed_room_11, '2026-10-14 12:00:00', '2026-10-14 13:38:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-14 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Kiên Giang (Rạch Sỏi); room 02; local 2026-10-17 19:00:00 +0700
-- source showtime_id: e39355d6-5138-422b-a870-9f5d03b1c7de
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cfcf127b46eb5958852f0807c22228a4') OR (room_id = @cinestar_seed_room_11 AND (starts_at = '2026-10-17 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-17 13:38:00' AND ends_at > '2026-10-17 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cfcf127b46eb5958852f0807c22228a4'), @cinestar_seed_movie_12, @cinestar_seed_room_11, '2026-10-17 12:00:00', '2026-10-17 13:38:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_11 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-17 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 03; local 2026-10-05 12:40:00 +0700
-- source showtime_id: 0e3965e0-69fe-46c6-af24-266893d6e96d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e68011242d685b3ea58a7fa14f4cb135') OR (room_id = @cinestar_seed_room_12 AND (starts_at = '2026-10-05 05:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:55:00' AND ends_at > '2026-10-05 05:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e68011242d685b3ea58a7fa14f4cb135'), @cinestar_seed_movie_15, @cinestar_seed_room_12, '2026-10-05 05:40:00', '2026-10-05 07:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_12 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 03; local 2026-10-05 15:20:00 +0700
-- source showtime_id: 3b944afe-8fc3-48c3-99f3-4eeaa8ecd812
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('43579697f6c75bfe9daa3eac8b8a38b3') OR (room_id = @cinestar_seed_room_12 AND (starts_at = '2026-10-05 08:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:35:00' AND ends_at > '2026-10-05 08:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('43579697f6c75bfe9daa3eac8b8a38b3'), @cinestar_seed_movie_15, @cinestar_seed_room_12, '2026-10-05 08:20:00', '2026-10-05 10:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_12 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 03; local 2026-10-05 18:00:00 +0700
-- source showtime_id: 52ff3d3e-fb7a-41b5-bb49-032ef7d39692
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f45b2dbb8bbc55028dac9d813fad39ab') OR (room_id = @cinestar_seed_room_12 AND (starts_at = '2026-10-05 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:15:00' AND ends_at > '2026-10-05 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f45b2dbb8bbc55028dac9d813fad39ab'), @cinestar_seed_movie_15, @cinestar_seed_room_12, '2026-10-05 11:00:00', '2026-10-05 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_12 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 03; local 2026-10-05 20:40:00 +0700
-- source showtime_id: 54734748-6a99-432a-995e-9e1d4e25a6b0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2133f158b5e657519fc7c04597cbaf56') OR (room_id = @cinestar_seed_room_12 AND (starts_at = '2026-10-05 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:55:00' AND ends_at > '2026-10-05 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2133f158b5e657519fc7c04597cbaf56'), @cinestar_seed_movie_15, @cinestar_seed_room_12, '2026-10-05 13:40:00', '2026-10-05 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_12 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 03; local 2026-10-05 23:20:00 +0700
-- source showtime_id: 5f646e2a-79a6-4219-992b-0824cc62de2a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('91dde53be3f9582fa7311b34ae5ee84d') OR (room_id = @cinestar_seed_room_12 AND (starts_at = '2026-10-05 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:35:00' AND ends_at > '2026-10-05 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('91dde53be3f9582fa7311b34ae5ee84d'), @cinestar_seed_movie_15, @cinestar_seed_room_12, '2026-10-05 16:20:00', '2026-10-05 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_12 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 05; local 2026-10-05 11:20:00 +0700
-- source showtime_id: 8c594ae7-1c29-4ded-8c38-13080c19c5e5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('94c478f9d9535e6fb2eb5d0cf8400ed6') OR (room_id = @cinestar_seed_room_13 AND (starts_at = '2026-10-05 04:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:35:00' AND ends_at > '2026-10-05 04:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('94c478f9d9535e6fb2eb5d0cf8400ed6'), @cinestar_seed_movie_15, @cinestar_seed_room_13, '2026-10-05 04:20:00', '2026-10-05 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_13 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 05; local 2026-10-05 14:00:00 +0700
-- source showtime_id: fa152eab-8e04-483b-b140-6ced495917ca
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0531f7b8a8a9508cb58eee768e80494f') OR (room_id = @cinestar_seed_room_13 AND (starts_at = '2026-10-05 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:15:00' AND ends_at > '2026-10-05 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0531f7b8a8a9508cb58eee768e80494f'), @cinestar_seed_movie_15, @cinestar_seed_room_13, '2026-10-05 07:00:00', '2026-10-05 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_13 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 05; local 2026-10-05 16:40:00 +0700
-- source showtime_id: 63e92137-a94e-49de-91eb-a790a744474c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('32493d3e69f4584095ea5b3ce68186f1') OR (room_id = @cinestar_seed_room_13 AND (starts_at = '2026-10-05 09:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:55:00' AND ends_at > '2026-10-05 09:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('32493d3e69f4584095ea5b3ce68186f1'), @cinestar_seed_movie_15, @cinestar_seed_room_13, '2026-10-05 09:40:00', '2026-10-05 11:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_13 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 05; local 2026-10-05 19:20:00 +0700
-- source showtime_id: 71a76e7a-a526-4652-bad4-1310026a6aa3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7a38fcf5211152e2bade8129d17ee0fb') OR (room_id = @cinestar_seed_room_13 AND (starts_at = '2026-10-05 12:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:35:00' AND ends_at > '2026-10-05 12:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7a38fcf5211152e2bade8129d17ee0fb'), @cinestar_seed_movie_15, @cinestar_seed_room_13, '2026-10-05 12:20:00', '2026-10-05 14:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_13 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Kiên Giang (Rạch Sỏi); room 05; local 2026-10-05 22:00:00 +0700
-- source showtime_id: ab9cb3bb-29a0-4936-a5b7-45bad10a5f36
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9f2c5a4704d65d0a8ff9ff209b7e7699') OR (room_id = @cinestar_seed_room_13 AND (starts_at = '2026-10-05 15:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:15:00' AND ends_at > '2026-10-05 15:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9f2c5a4704d65d0a8ff9ff209b7e7699'), @cinestar_seed_movie_15, @cinestar_seed_room_13, '2026-10-05 15:00:00', '2026-10-05 17:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_13 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-05 12:30:00 +0700
-- source showtime_id: e8dff06c-f1ed-4ebc-916c-924813c543d3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('258f170f7ae352d8a253e85ce7c745d8') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-05 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:45:00' AND ends_at > '2026-10-05 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('258f170f7ae352d8a253e85ce7c745d8'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-05 05:30:00', '2026-10-05 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-05 15:10:00 +0700
-- source showtime_id: a52d4fd1-637f-4a23-a638-c373da1149af
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b5d322dd436a5a3389043d3f5f046c07') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-05 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:25:00' AND ends_at > '2026-10-05 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b5d322dd436a5a3389043d3f5f046c07'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-05 08:10:00', '2026-10-05 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-05 17:50:00 +0700
-- source showtime_id: 1e4db556-84de-4273-9aec-07257460bb78
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('37c4c28fb6b858ddb9d440c19595f33f') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-05 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:05:00' AND ends_at > '2026-10-05 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('37c4c28fb6b858ddb9d440c19595f33f'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-05 10:50:00', '2026-10-05 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-05 20:30:00 +0700
-- source showtime_id: 1f71dd4d-4302-4bd9-88ed-7f18a0ad4ccf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('05f5ad33d40c5f268cf6e47c16777211') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-05 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:45:00' AND ends_at > '2026-10-05 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('05f5ad33d40c5f268cf6e47c16777211'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-05 13:30:00', '2026-10-05 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-05 23:10:00 +0700
-- source showtime_id: 9ba0ac3f-a045-470d-ac51-347c442e8b32
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('313c8d1e1c645982a016bd22598de155') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-05 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:25:00' AND ends_at > '2026-10-05 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('313c8d1e1c645982a016bd22598de155'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-05 16:10:00', '2026-10-05 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 08:05:00 +0700
-- source showtime_id: 0a9df3af-db14-44b5-ae6a-d9c2401db474
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4c691bca297f5b81b2813d144cb4b9bb') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 01:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:21:00' AND ends_at > '2026-10-06 01:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4c691bca297f5b81b2813d144cb4b9bb'), @cinestar_seed_movie_02, @cinestar_seed_room_14, '2026-10-06 01:05:00', '2026-10-06 02:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 09:50:00 +0700
-- source showtime_id: 34952cc6-3ac9-4ad1-a22a-4d47b780f868
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('faa3f6193d85586cbb89a823e3fe24ac') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:05:00' AND ends_at > '2026-10-06 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('faa3f6193d85586cbb89a823e3fe24ac'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 02:50:00', '2026-10-06 05:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 12:30:00 +0700
-- source showtime_id: 8d77044a-b1d2-4619-9ae6-82f98c48dbfe
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c7f94359ba5955bb8b328db30649b9aa') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:45:00' AND ends_at > '2026-10-06 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c7f94359ba5955bb8b328db30649b9aa'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 05:30:00', '2026-10-06 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 15:10:00 +0700
-- source showtime_id: 511d8aa7-3c29-43a5-96c4-6af33ec04774
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('055021930d475df3bb677b4a990912d2') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:25:00' AND ends_at > '2026-10-06 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('055021930d475df3bb677b4a990912d2'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 08:10:00', '2026-10-06 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 17:50:00 +0700
-- source showtime_id: f483c854-642a-4b79-b097-9bc81796af54
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9d2ddc9744835cfcb83b1e841e1bf661') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:05:00' AND ends_at > '2026-10-06 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9d2ddc9744835cfcb83b1e841e1bf661'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 10:50:00', '2026-10-06 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 20:30:00 +0700
-- source showtime_id: 9e89ec0f-6c34-4720-985e-b8cc984d73c8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b6827ca1322e5438977627b1f365365c') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:45:00' AND ends_at > '2026-10-06 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b6827ca1322e5438977627b1f365365c'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 13:30:00', '2026-10-06 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 01; local 2026-10-06 23:10:00 +0700
-- source showtime_id: 33a04857-e20d-4dc6-8b08-44ac927ea78e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e95616d7552d5c4bb11faf3b7c1857d3') OR (room_id = @cinestar_seed_room_14 AND (starts_at = '2026-10-06 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:25:00' AND ends_at > '2026-10-06 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e95616d7552d5c4bb11faf3b7c1857d3'), @cinestar_seed_movie_15, @cinestar_seed_room_14, '2026-10-06 16:10:00', '2026-10-06 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_14 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-05 12:00:00 +0700
-- source showtime_id: 03f1eb1d-2ce2-44ff-a8a0-7a477df1d7d6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c3ea05af253b53669c475cf6e95026c0') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-05 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:15:00' AND ends_at > '2026-10-05 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c3ea05af253b53669c475cf6e95026c0'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-05 05:00:00', '2026-10-05 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-05 14:40:00 +0700
-- source showtime_id: bc79c27f-1e50-4460-a947-2769d436387c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e241425adbd25ddb8f9d352491600bf1') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-05 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:55:00' AND ends_at > '2026-10-05 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e241425adbd25ddb8f9d352491600bf1'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-05 07:40:00', '2026-10-05 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-05 17:20:00 +0700
-- source showtime_id: 480bad71-b4ba-4ba4-847c-062e0857b9a0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('72a30169602a57cbaf57809f4cbd148b') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-05 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:35:00' AND ends_at > '2026-10-05 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('72a30169602a57cbaf57809f4cbd148b'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-05 10:20:00', '2026-10-05 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-05 20:00:00 +0700
-- source showtime_id: b14f294e-8176-43ae-b5e6-cd4298e980e8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('eadbd824b2e75a55b77862aac378f1e3') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-05 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:15:00' AND ends_at > '2026-10-05 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('eadbd824b2e75a55b77862aac378f1e3'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-05 13:00:00', '2026-10-05 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-05 22:40:00 +0700
-- source showtime_id: f5a1e57e-1b01-4926-81b3-f907765f9c88
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('aebc1f7ba166549eb5ea70ec741a3928') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:55:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('aebc1f7ba166549eb5ea70ec741a3928'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-05 15:40:00', '2026-10-05 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 08:15:00 +0700
-- source showtime_id: 1223e526-067e-432b-abec-af8c391e18c5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1bb0ec59fbc858a4b258a1d8a3229d70') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 01:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:35:00' AND ends_at > '2026-10-06 01:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1bb0ec59fbc858a4b258a1d8a3229d70'), @cinestar_seed_movie_08, @cinestar_seed_room_15, '2026-10-06 01:15:00', '2026-10-06 02:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 10:00:00 +0700
-- source showtime_id: 227d0cc9-17d5-43c3-a34d-45a134732a52
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c14fa3cad03d5ec0a117509e5972c2ac') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 03:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:36:00' AND ends_at > '2026-10-06 03:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c14fa3cad03d5ec0a117509e5972c2ac'), @cinestar_seed_movie_06, @cinestar_seed_room_15, '2026-10-06 03:00:00', '2026-10-06 04:36:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 12:00:00 +0700
-- source showtime_id: 07a58351-ca57-4a34-9905-8f86e5cc3027
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6bd998d724605081b7fae9fee0f52ac9') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:15:00' AND ends_at > '2026-10-06 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6bd998d724605081b7fae9fee0f52ac9'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-06 05:00:00', '2026-10-06 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 14:40:00 +0700
-- source showtime_id: 5b643c94-2062-468d-89fe-3577b0ecaadd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('478fc94cc5c95587b611880af6f6dc97') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:55:00' AND ends_at > '2026-10-06 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('478fc94cc5c95587b611880af6f6dc97'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-06 07:40:00', '2026-10-06 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 17:20:00 +0700
-- source showtime_id: aaff9207-ebc0-4bcd-8de9-6364303a9427
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7c8ac3b7c4c452fb85aa803ad4e40b50') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:35:00' AND ends_at > '2026-10-06 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7c8ac3b7c4c452fb85aa803ad4e40b50'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-06 10:20:00', '2026-10-06 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 20:00:00 +0700
-- source showtime_id: bba1908c-b165-473b-b0bb-3a091e468ca7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('febbb0b2828455289e426701e9817d4d') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:15:00' AND ends_at > '2026-10-06 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('febbb0b2828455289e426701e9817d4d'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-06 13:00:00', '2026-10-06 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 02; local 2026-10-06 22:40:00 +0700
-- source showtime_id: 42db9eec-0a99-4893-9d05-5cec2383e3ac
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('73d5bcbb084c5452a204e9aab09d8e7e') OR (room_id = @cinestar_seed_room_15 AND (starts_at = '2026-10-06 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:55:00' AND ends_at > '2026-10-06 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('73d5bcbb084c5452a204e9aab09d8e7e'), @cinestar_seed_movie_15, @cinestar_seed_room_15, '2026-10-06 15:40:00', '2026-10-06 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_15 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-05 11:30:00 +0700
-- source showtime_id: a4b5577a-5bd4-440a-afa8-069f93523c93
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f8ab28f17bb35825af3f31823b484873') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-05 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:45:00' AND ends_at > '2026-10-05 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f8ab28f17bb35825af3f31823b484873'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-05 04:30:00', '2026-10-05 06:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-05 14:10:00 +0700
-- source showtime_id: 015c4a11-7441-42de-8964-6be0b8a219cb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('69e8c0029b8c552bb6ae7ee24b83e27b') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-05 07:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:25:00' AND ends_at > '2026-10-05 07:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('69e8c0029b8c552bb6ae7ee24b83e27b'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-05 07:10:00', '2026-10-05 09:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-05 16:50:00 +0700
-- source showtime_id: 1c9cb513-ba32-453b-8917-f6202639e715
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3b8eb62184ec5592b28cfb294ede23fe') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-05 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:05:00' AND ends_at > '2026-10-05 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3b8eb62184ec5592b28cfb294ede23fe'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-05 09:50:00', '2026-10-05 12:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-05 19:30:00 +0700
-- source showtime_id: 4adbe8e4-1702-4aba-8a55-258265edf8db
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('176da58093ee501a91cd2c8f58d1f344') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-05 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:45:00' AND ends_at > '2026-10-05 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('176da58093ee501a91cd2c8f58d1f344'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-05 12:30:00', '2026-10-05 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-05 22:10:00 +0700
-- source showtime_id: 64f75919-61e9-480f-9bfc-ce2b4373ba73
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8bad1e0ea3c153fb8fa3098bc2a495f1') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-05 15:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:25:00' AND ends_at > '2026-10-05 15:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8bad1e0ea3c153fb8fa3098bc2a495f1'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-05 15:10:00', '2026-10-05 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 08:50:00 +0700
-- source showtime_id: cd5d5c50-5054-439b-a3ae-3291cae5e0bf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('de3b38e45ddd52d28944e6f2467b57de') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 01:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:05:00' AND ends_at > '2026-10-06 01:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('de3b38e45ddd52d28944e6f2467b57de'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 01:50:00', '2026-10-06 04:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 11:30:00 +0700
-- source showtime_id: 0500ad0f-b644-4f66-b265-5e6909441f03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('dd2e2151b99e5a5a860359c1a984f75b') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:45:00' AND ends_at > '2026-10-06 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('dd2e2151b99e5a5a860359c1a984f75b'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 04:30:00', '2026-10-06 06:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 14:10:00 +0700
-- source showtime_id: d8dce972-9164-43b1-be5c-25c8c8e004f5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c7c24a8fc3ce502d874800d47a9d3708') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 07:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:25:00' AND ends_at > '2026-10-06 07:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c7c24a8fc3ce502d874800d47a9d3708'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 07:10:00', '2026-10-06 09:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 16:50:00 +0700
-- source showtime_id: f5f5fbe3-0e03-4942-a42e-c285d3e340bb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5b726f81d81b5a1494ed704ca7d64431') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:05:00' AND ends_at > '2026-10-06 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5b726f81d81b5a1494ed704ca7d64431'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 09:50:00', '2026-10-06 12:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 19:30:00 +0700
-- source showtime_id: 5dcb2334-87b9-42e9-97b9-bf85cd302c52
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d1b9ca6cef1458fdb353c0ccefcd2484') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:45:00' AND ends_at > '2026-10-06 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d1b9ca6cef1458fdb353c0ccefcd2484'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 12:30:00', '2026-10-06 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 03; local 2026-10-06 22:10:00 +0700
-- source showtime_id: a0c12833-2888-486a-b877-515ac3442531
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('48bf08efff7050c49d709c2c48c757be') OR (room_id = @cinestar_seed_room_16 AND (starts_at = '2026-10-06 15:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:25:00' AND ends_at > '2026-10-06 15:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('48bf08efff7050c49d709c2c48c757be'), @cinestar_seed_movie_15, @cinestar_seed_room_16, '2026-10-06 15:10:00', '2026-10-06 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_16 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 11:45:00 +0700
-- source showtime_id: b07a94df-c4c2-4cb0-9154-863aff29fcd2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('52ae3b9205ac5e36b346184679929f24') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 04:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:26:00' AND ends_at > '2026-10-05 04:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('52ae3b9205ac5e36b346184679929f24'), @cinestar_seed_movie_29, @cinestar_seed_room_17, '2026-10-05 04:45:00', '2026-10-05 06:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 13:50:00 +0700
-- source showtime_id: 856fd365-e0db-448a-ac63-05d23fa587a1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('42367a64aa195faa80a3bc8cf604fce4') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:52:00' AND ends_at > '2026-10-05 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('42367a64aa195faa80a3bc8cf604fce4'), @cinestar_seed_movie_05, @cinestar_seed_room_17, '2026-10-05 06:50:00', '2026-10-05 08:52:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 16:20:00 +0700
-- source showtime_id: 7f842a67-23bb-4129-a0d4-8fb60b1df86c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2551fe920a965d2585f2243337cf3182') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:15:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2551fe920a965d2585f2243337cf3182'), @cinestar_seed_movie_07, @cinestar_seed_room_17, '2026-10-05 09:20:00', '2026-10-05 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 18:40:00 +0700
-- source showtime_id: 532e74f1-a875-40e0-a1bb-156b19b86c47
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4eb66d68f754500c897945b35c182ffd') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:55:00' AND ends_at > '2026-10-05 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4eb66d68f754500c897945b35c182ffd'), @cinestar_seed_movie_15, @cinestar_seed_room_17, '2026-10-05 11:40:00', '2026-10-05 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 21:20:00 +0700
-- source showtime_id: 4877a66d-c9fc-4170-b37f-e25a8667f91e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1a2c5ff2eac75da888c418d8ea4c0425') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:35:00' AND ends_at > '2026-10-05 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1a2c5ff2eac75da888c418d8ea4c0425'), @cinestar_seed_movie_15, @cinestar_seed_room_17, '2026-10-05 14:20:00', '2026-10-05 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-05 23:59:00 +0700
-- source showtime_id: 35298bbe-6885-46a9-9485-b11fd7bdd951
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('39bd3c559bd2547f9d1dedc30aaafec8') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-05 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:40:00' AND ends_at > '2026-10-05 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('39bd3c559bd2547f9d1dedc30aaafec8'), @cinestar_seed_movie_29, @cinestar_seed_room_17, '2026-10-05 16:59:00', '2026-10-05 18:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 08:15:00 +0700
-- source showtime_id: 7995b53b-1a4f-4211-9506-e34a25b4274b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0b5add94b7e75eee8f6cb02231aa1dfa') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 01:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:18:00' AND ends_at > '2026-10-06 01:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0b5add94b7e75eee8f6cb02231aa1dfa'), @cinestar_seed_movie_20, @cinestar_seed_room_17, '2026-10-06 01:15:00', '2026-10-06 04:18:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 11:45:00 +0700
-- source showtime_id: fb3630ff-5e11-48f5-aeb7-ff0d1af0d8e3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d45e8b91737654b49d11ac3da0a78966') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 04:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:26:00' AND ends_at > '2026-10-06 04:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d45e8b91737654b49d11ac3da0a78966'), @cinestar_seed_movie_29, @cinestar_seed_room_17, '2026-10-06 04:45:00', '2026-10-06 06:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 13:50:00 +0700
-- source showtime_id: 7cb2a1b7-92e0-45b9-a9a8-c66419475092
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7dcddb7b67115d6ead6bab653c70083c') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:52:00' AND ends_at > '2026-10-06 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7dcddb7b67115d6ead6bab653c70083c'), @cinestar_seed_movie_05, @cinestar_seed_room_17, '2026-10-06 06:50:00', '2026-10-06 08:52:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 16:20:00 +0700
-- source showtime_id: 4f3f36f6-4f00-4148-b774-d067696b9c40
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d77f9c5c76815657ac46c36ef82e7f01') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:15:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d77f9c5c76815657ac46c36ef82e7f01'), @cinestar_seed_movie_07, @cinestar_seed_room_17, '2026-10-06 09:20:00', '2026-10-06 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 18:40:00 +0700
-- source showtime_id: 374af6af-a49b-491f-8f06-82ef820a3402
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ad3ca254a9c55ba182781b5187ebbe59') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:55:00' AND ends_at > '2026-10-06 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ad3ca254a9c55ba182781b5187ebbe59'), @cinestar_seed_movie_15, @cinestar_seed_room_17, '2026-10-06 11:40:00', '2026-10-06 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 21:20:00 +0700
-- source showtime_id: dc2fbbcd-1d94-44f0-9dab-67e3afc06ccd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4d29874eda4e5e67b2949306a2dd6d13') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:35:00' AND ends_at > '2026-10-06 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4d29874eda4e5e67b2949306a2dd6d13'), @cinestar_seed_movie_15, @cinestar_seed_room_17, '2026-10-06 14:20:00', '2026-10-06 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Hiệp Phú (TP.HCM); room 05; local 2026-10-06 23:59:00 +0700
-- source showtime_id: eb08190a-5faf-4464-b894-4c718ed1c41d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ff40c941b6d55374ae9a200db4f040f8') OR (room_id = @cinestar_seed_room_17 AND (starts_at = '2026-10-06 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:40:00' AND ends_at > '2026-10-06 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ff40c941b6d55374ae9a200db4f040f8'), @cinestar_seed_movie_29, @cinestar_seed_room_17, '2026-10-06 16:59:00', '2026-10-06 18:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_17 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 12:20:00 +0700
-- source showtime_id: c3a502b8-13fc-45ce-8d33-409ae5cd26f6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ea71223efab9597c9724a2c57c2395c7') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 05:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:01:00' AND ends_at > '2026-10-05 05:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ea71223efab9597c9724a2c57c2395c7'), @cinestar_seed_movie_29, @cinestar_seed_room_18, '2026-10-05 05:20:00', '2026-10-05 07:01:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 14:30:00 +0700
-- source showtime_id: 77185397-7e27-4b3f-b67f-9a50124c1271
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('45ff1017bbca51bcace79e17e37150e6') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 07:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:50:00' AND ends_at > '2026-10-05 07:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('45ff1017bbca51bcace79e17e37150e6'), @cinestar_seed_movie_08, @cinestar_seed_room_18, '2026-10-05 07:30:00', '2026-10-05 08:50:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 16:20:00 +0700
-- source showtime_id: f65b3d2b-ce08-4ef6-abee-f55b89b24a28
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f3ee4de43df250ccb5b80c507c466b14') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:15:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f3ee4de43df250ccb5b80c507c466b14'), @cinestar_seed_movie_07, @cinestar_seed_room_18, '2026-10-05 09:20:00', '2026-10-05 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 18:40:00 +0700
-- source showtime_id: 2f6404af-f7b9-4e71-93cb-14ffa942af74
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('25d2026ebc755137a284e37f469daf25') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:55:00' AND ends_at > '2026-10-05 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('25d2026ebc755137a284e37f469daf25'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-05 11:40:00', '2026-10-05 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 21:20:00 +0700
-- source showtime_id: ead31d88-9b5e-4333-8c95-52ac8dbb9777
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a0d78c9d76ad5f039ce70ff0099363a5') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:35:00' AND ends_at > '2026-10-05 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a0d78c9d76ad5f039ce70ff0099363a5'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-05 14:20:00', '2026-10-05 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-05 23:59:00 +0700
-- source showtime_id: 16ac5e31-988d-434f-94ba-fd806d5fce36
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('159a82bbe2b95fa99c8f540faaa1033c') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-05 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 19:14:00' AND ends_at > '2026-10-05 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('159a82bbe2b95fa99c8f540faaa1033c'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-05 16:59:00', '2026-10-05 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Parkcity Hà Nội; room 01; local 2026-10-06 14:30:00 +0700
-- source showtime_id: b3be4d62-81ae-4b7e-8cb6-28c61a773e90
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0b0f158f58935681b8966a1ab51c12f3') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-06 07:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:50:00' AND ends_at > '2026-10-06 07:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0b0f158f58935681b8966a1ab51c12f3'), @cinestar_seed_movie_08, @cinestar_seed_room_18, '2026-10-06 07:30:00', '2026-10-06 08:50:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Parkcity Hà Nội; room 01; local 2026-10-06 16:20:00 +0700
-- source showtime_id: a1bb9d1c-e286-454a-8cb5-2cc236a5f50d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f8f74d36417759daab4b1212c0358947') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:15:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f8f74d36417759daab4b1212c0358947'), @cinestar_seed_movie_07, @cinestar_seed_room_18, '2026-10-06 09:20:00', '2026-10-06 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-06 18:40:00 +0700
-- source showtime_id: 7ec63b66-6221-4a30-b131-4cb7d9580ed2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0a640a5c65b05b0eb5ec8cd9954779f0') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-06 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:55:00' AND ends_at > '2026-10-06 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0a640a5c65b05b0eb5ec8cd9954779f0'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-06 11:40:00', '2026-10-06 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-06 21:20:00 +0700
-- source showtime_id: 77237f51-6cbf-40ac-bd5f-443616452b57
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6e06341f3b0d5b38a49cb415b2fda634') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-06 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:35:00' AND ends_at > '2026-10-06 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6e06341f3b0d5b38a49cb415b2fda634'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-06 14:20:00', '2026-10-06 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 01; local 2026-10-06 23:59:00 +0700
-- source showtime_id: 74513b62-c17f-491c-9d6d-497e86f061cb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03334e8cd0d052ed9b72d827b66eb677') OR (room_id = @cinestar_seed_room_18 AND (starts_at = '2026-10-06 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 19:14:00' AND ends_at > '2026-10-06 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03334e8cd0d052ed9b72d827b66eb677'), @cinestar_seed_movie_15, @cinestar_seed_room_18, '2026-10-06 16:59:00', '2026-10-06 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_18 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Parkcity Hà Nội; room 02; local 2026-10-05 11:10:00 +0700
-- source showtime_id: e43a96d4-3246-45f7-ad07-d2c3b0b1458e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('93b2625dfc395f7093d587b26f4c79e1') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-05 04:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:12:00' AND ends_at > '2026-10-05 04:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('93b2625dfc395f7093d587b26f4c79e1'), @cinestar_seed_movie_05, @cinestar_seed_room_19, '2026-10-05 04:10:00', '2026-10-05 06:12:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-05 13:40:00 +0700
-- source showtime_id: 0ce9b51b-4fc6-4c5b-92cb-81a4ef8ec419
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2298c07bb26751aea918206d781866c5') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-05 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:35:00' AND ends_at > '2026-10-05 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2298c07bb26751aea918206d781866c5'), @cinestar_seed_movie_07, @cinestar_seed_room_19, '2026-10-05 06:40:00', '2026-10-05 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-05 16:00:00 +0700
-- source showtime_id: 2cee61b6-8293-4b59-8b28-f48a5be6120b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f1022e9b93c356ceaa8048f80cd0af64') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-05 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:41:00' AND ends_at > '2026-10-05 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f1022e9b93c356ceaa8048f80cd0af64'), @cinestar_seed_movie_29, @cinestar_seed_room_19, '2026-10-05 09:00:00', '2026-10-05 10:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-05 18:10:00 +0700
-- source showtime_id: 6b0c52be-340b-4e02-925a-a8d89e3ab144
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5853bc3840d85ae6b6379c25311cf166') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-05 11:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:13:00' AND ends_at > '2026-10-05 11:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5853bc3840d85ae6b6379c25311cf166'), @cinestar_seed_movie_20, @cinestar_seed_room_19, '2026-10-05 11:10:00', '2026-10-05 14:13:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 02; local 2026-10-05 21:40:00 +0700
-- source showtime_id: ca0d162b-d82e-4906-b8b4-75b2f98b1f03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0787d895d5e55fa5ace414ab4668e2e2') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-05 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:55:00' AND ends_at > '2026-10-05 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0787d895d5e55fa5ace414ab4668e2e2'), @cinestar_seed_movie_15, @cinestar_seed_room_19, '2026-10-05 14:40:00', '2026-10-05 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 08:50:00 +0700
-- source showtime_id: 01b9fb52-073c-4f6f-89cb-fb4ef612ddfd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('92c2aaeef33050e1b2640a75bd257056') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 01:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:45:00' AND ends_at > '2026-10-06 01:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('92c2aaeef33050e1b2640a75bd257056'), @cinestar_seed_movie_07, @cinestar_seed_room_19, '2026-10-06 01:50:00', '2026-10-06 03:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 11:10:00 +0700
-- source showtime_id: 676cff38-e8c4-4b8f-ad67-efebb4b46afc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c9a43547c26d5fc0abdbe0b5c0682078') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 04:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:12:00' AND ends_at > '2026-10-06 04:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c9a43547c26d5fc0abdbe0b5c0682078'), @cinestar_seed_movie_05, @cinestar_seed_room_19, '2026-10-06 04:10:00', '2026-10-06 06:12:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 13:40:00 +0700
-- source showtime_id: f070d055-6771-4b14-b6fa-ca7103fafa31
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('540014972c0b50bc9f8d9ca93f7f387f') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:35:00' AND ends_at > '2026-10-06 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('540014972c0b50bc9f8d9ca93f7f387f'), @cinestar_seed_movie_07, @cinestar_seed_room_19, '2026-10-06 06:40:00', '2026-10-06 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 16:00:00 +0700
-- source showtime_id: fee8534b-4877-4d99-9d60-0fc50bac7a19
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('63696850acc755c284a0dbc784157962') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:41:00' AND ends_at > '2026-10-06 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('63696850acc755c284a0dbc784157962'), @cinestar_seed_movie_29, @cinestar_seed_room_19, '2026-10-06 09:00:00', '2026-10-06 10:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 18:10:00 +0700
-- source showtime_id: eba77bc2-bbbc-4bda-bab6-fdf285e6b306
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('772f73f6c9825b7db5f11ce75aed4692') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 11:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:13:00' AND ends_at > '2026-10-06 11:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('772f73f6c9825b7db5f11ce75aed4692'), @cinestar_seed_movie_20, @cinestar_seed_room_19, '2026-10-06 11:10:00', '2026-10-06 14:13:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 02; local 2026-10-06 21:40:00 +0700
-- source showtime_id: 67d99c4b-5666-42e3-9798-356d3ff291c7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f536ba339bfc58c892671b3b1e48c59e') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-06 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:55:00' AND ends_at > '2026-10-06 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f536ba339bfc58c892671b3b1e48c59e'), @cinestar_seed_movie_15, @cinestar_seed_room_19, '2026-10-06 14:40:00', '2026-10-06 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Parkcity Hà Nội; room 02; local 2026-10-12 19:00:00 +0700
-- source showtime_id: c44213ea-63f9-4776-a5d6-3a6d1d00c88e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('da291880dea35e9eb0689e0cef1fb62c') OR (room_id = @cinestar_seed_room_19 AND (starts_at = '2026-10-12 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-12 13:38:00' AND ends_at > '2026-10-12 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('da291880dea35e9eb0689e0cef1fb62c'), @cinestar_seed_movie_12, @cinestar_seed_room_19, '2026-10-12 12:00:00', '2026-10-12 13:38:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_19 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-12 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-05 12:40:00 +0700
-- source showtime_id: 4098bb82-49a4-4e5b-8bbc-e881fb4659d2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('715545dc1ee45d998bda74ceee703364') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-05 05:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:55:00' AND ends_at > '2026-10-05 05:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('715545dc1ee45d998bda74ceee703364'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-05 05:40:00', '2026-10-05 07:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-05 15:20:00 +0700
-- source showtime_id: d88ce8cf-9e37-43e4-bb3c-251cba640a5f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0c53ab296a96576ca0a3c12b5892c846') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-05 08:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:35:00' AND ends_at > '2026-10-05 08:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0c53ab296a96576ca0a3c12b5892c846'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-05 08:20:00', '2026-10-05 10:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-05 18:00:00 +0700
-- source showtime_id: 4f9b36ff-68cc-40ea-b2e1-312f6a7e69f2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('225bb5d8a4f7569aab55aa114d63c506') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-05 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:15:00' AND ends_at > '2026-10-05 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('225bb5d8a4f7569aab55aa114d63c506'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-05 11:00:00', '2026-10-05 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-05 20:40:00 +0700
-- source showtime_id: 703ad5cc-7f11-4e03-8154-89ebed56aee1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('524da4ef7d5952ccb4fbb3ea3bb37c8a') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-05 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:55:00' AND ends_at > '2026-10-05 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('524da4ef7d5952ccb4fbb3ea3bb37c8a'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-05 13:40:00', '2026-10-05 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-05 23:20:00 +0700
-- source showtime_id: d550316e-66ee-4f0e-bcbf-8d4649548da8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d23d15a69a565767a5bb9c7670b202aa') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-05 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:35:00' AND ends_at > '2026-10-05 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d23d15a69a565767a5bb9c7670b202aa'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-05 16:20:00', '2026-10-05 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 821988bb-c2cc-4888-ad86-521feb483bc3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('32c4c353561c5ce1aa389643eace3426') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:36:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('32c4c353561c5ce1aa389643eace3426'), @cinestar_seed_movie_06, @cinestar_seed_room_20, '2026-10-06 01:00:00', '2026-10-06 02:36:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 10:00:00 +0700
-- source showtime_id: f8f2eef3-9395-48db-b4c5-b4c9d40b5860
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0993761d0c2050b99ce37d0ba1523272') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 03:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:15:00' AND ends_at > '2026-10-06 03:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0993761d0c2050b99ce37d0ba1523272'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 03:00:00', '2026-10-06 05:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 12:40:00 +0700
-- source showtime_id: 2eb1bd0f-7bc9-4d6a-9bef-65d7cc5a6352
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('915695cd4ba65fdeaa23977200809b33') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 05:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:55:00' AND ends_at > '2026-10-06 05:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('915695cd4ba65fdeaa23977200809b33'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 05:40:00', '2026-10-06 07:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 15:20:00 +0700
-- source showtime_id: f3f4ce77-7398-4d29-8f89-2b409e2abaff
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4bb13bf91c2f5af5b2cacc3137d35507') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 08:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:35:00' AND ends_at > '2026-10-06 08:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4bb13bf91c2f5af5b2cacc3137d35507'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 08:20:00', '2026-10-06 10:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 18:00:00 +0700
-- source showtime_id: 6e524f05-ca11-4fec-8de9-62431ec36b6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2e1fd987d9b45e7cbc8f539bcc367864') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:15:00' AND ends_at > '2026-10-06 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2e1fd987d9b45e7cbc8f539bcc367864'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 11:00:00', '2026-10-06 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 20:40:00 +0700
-- source showtime_id: aed14e05-37d4-44b3-9d15-55c95fe063c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('be1669ea653f5e019b2003393ff620d9') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:55:00' AND ends_at > '2026-10-06 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('be1669ea653f5e019b2003393ff620d9'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 13:40:00', '2026-10-06 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 05; local 2026-10-06 23:20:00 +0700
-- source showtime_id: 77320838-1eb5-4aae-baf5-b2b2efaba885
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('21af90969cf450c398eca4450990378b') OR (room_id = @cinestar_seed_room_20 AND (starts_at = '2026-10-06 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:35:00' AND ends_at > '2026-10-06 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('21af90969cf450c398eca4450990378b'), @cinestar_seed_movie_15, @cinestar_seed_room_20, '2026-10-06 16:20:00', '2026-10-06 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_20 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-05 11:20:00 +0700
-- source showtime_id: 8b1bbeda-845c-46fe-9dec-ce569852a7a4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b6d739ed79aa5a64bd33bfe5a34703f8') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-05 04:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:35:00' AND ends_at > '2026-10-05 04:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b6d739ed79aa5a64bd33bfe5a34703f8'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-05 04:20:00', '2026-10-05 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-05 14:00:00 +0700
-- source showtime_id: f64decbe-155c-4726-8389-535d32e21d0b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c598f087f5e05b859b2876c8a3a58d9c') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-05 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:15:00' AND ends_at > '2026-10-05 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c598f087f5e05b859b2876c8a3a58d9c'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-05 07:00:00', '2026-10-05 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-05 16:40:00 +0700
-- source showtime_id: 24c0a5ed-81e2-4c54-8aa7-53b178665afd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b4bef3e509b650e9abf0c901166827cb') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-05 09:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:55:00' AND ends_at > '2026-10-05 09:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b4bef3e509b650e9abf0c901166827cb'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-05 09:40:00', '2026-10-05 11:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-05 19:20:00 +0700
-- source showtime_id: ecc8c32a-0d9c-49d9-9584-d5d35f7e7e6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c57bd0c171e45986968718162c896829') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-05 12:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:35:00' AND ends_at > '2026-10-05 12:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c57bd0c171e45986968718162c896829'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-05 12:20:00', '2026-10-05 14:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-05 22:00:00 +0700
-- source showtime_id: 7c1f851c-45eb-4e47-bd22-59a4f7e93411
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3d153b1e86555b3ab1d2e0f92c0a9246') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-05 15:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:15:00' AND ends_at > '2026-10-05 15:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3d153b1e86555b3ab1d2e0f92c0a9246'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-05 15:00:00', '2026-10-05 17:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 08:40:00 +0700
-- source showtime_id: 4747dbd0-8dd6-49e3-8b18-ac6439ced467
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2c1f619332a653f49bac482e667d9358') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 01:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:55:00' AND ends_at > '2026-10-06 01:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2c1f619332a653f49bac482e667d9358'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 01:40:00', '2026-10-06 03:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 11:20:00 +0700
-- source showtime_id: 3cc365fa-b809-447d-b246-46c43b56243f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f8e03373f7025d8f81f0c40ed96b80f6') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 04:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:35:00' AND ends_at > '2026-10-06 04:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f8e03373f7025d8f81f0c40ed96b80f6'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 04:20:00', '2026-10-06 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 14:00:00 +0700
-- source showtime_id: 94216f48-252c-43e4-bd63-0e5889a4f879
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5a08b45b4d785b0b926e39127216db0c') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:15:00' AND ends_at > '2026-10-06 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5a08b45b4d785b0b926e39127216db0c'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 07:00:00', '2026-10-06 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 16:40:00 +0700
-- source showtime_id: c9d6440f-b069-45d9-84ff-fba991861faa
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('646ef088896c57ba8c6dfb76ecba24b5') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 09:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:55:00' AND ends_at > '2026-10-06 09:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('646ef088896c57ba8c6dfb76ecba24b5'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 09:40:00', '2026-10-06 11:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 19:20:00 +0700
-- source showtime_id: 2bfa1919-973a-4a45-be44-6ae83b59c852
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('65cd59b3f0935818bdb515f48e491477') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 12:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:35:00' AND ends_at > '2026-10-06 12:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('65cd59b3f0935818bdb515f48e491477'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 12:20:00', '2026-10-06 14:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Parkcity Hà Nội; room 06; local 2026-10-06 22:00:00 +0700
-- source showtime_id: 099de4d7-39d7-473f-9946-5c7cfb6d0ff6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c99bc3de4a425b23bceb018ce343aaf0') OR (room_id = @cinestar_seed_room_21 AND (starts_at = '2026-10-06 15:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:15:00' AND ends_at > '2026-10-06 15:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c99bc3de4a425b23bceb018ce343aaf0'), @cinestar_seed_movie_15, @cinestar_seed_room_21, '2026-10-06 15:00:00', '2026-10-06 17:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_21 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 12:15:00 +0700
-- source showtime_id: f4f83211-bed1-4d9b-b746-1fdce7059c53
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ace6a975691a5637a55bf5628f084183') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 05:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:56:00' AND ends_at > '2026-10-05 05:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ace6a975691a5637a55bf5628f084183'), @cinestar_seed_movie_29, @cinestar_seed_room_22, '2026-10-05 05:15:00', '2026-10-05 06:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 14:20:00 +0700
-- source showtime_id: 3025eadd-beeb-43f1-a689-b3591b286a94
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('21b18eda5ed85c39b580e560f9bc0c81') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:21:00' AND ends_at > '2026-10-05 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('21b18eda5ed85c39b580e560f9bc0c81'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-05 07:20:00', '2026-10-05 09:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 16:50:00 +0700
-- source showtime_id: fd41d74b-da28-4694-a25b-321f10d44a17
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8c62116da90d5f22b26ba64c795bea77') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:31:00' AND ends_at > '2026-10-05 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8c62116da90d5f22b26ba64c795bea77'), @cinestar_seed_movie_29, @cinestar_seed_room_22, '2026-10-05 09:50:00', '2026-10-05 11:31:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 18:50:00 +0700
-- source showtime_id: f4a67b68-2b29-4534-aa84-e7bf2009541d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('33a8ee84fe85512ba10f7a70deb792d5') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 11:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:45:00' AND ends_at > '2026-10-05 11:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), @cinestar_seed_movie_04, @cinestar_seed_room_22, '2026-10-05 11:50:00', '2026-10-05 13:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 21:10:00 +0700
-- source showtime_id: 9adf2d99-2b2c-43dd-bce3-e60d9a299b48
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('60d504387970554092dfcb7b09a08de4') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 14:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:11:00' AND ends_at > '2026-10-05 14:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('60d504387970554092dfcb7b09a08de4'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-05 14:10:00', '2026-10-05 16:11:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-05 23:40:00 +0700
-- source showtime_id: 9607b92c-b0b7-4d33-9fcf-ec4eedc92ba7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fb956458dee75f7897cb7bfd4fe1f6fd') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-05 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:41:00' AND ends_at > '2026-10-05 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fb956458dee75f7897cb7bfd4fe1f6fd'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-05 16:40:00', '2026-10-05 18:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 08:10:00 +0700
-- source showtime_id: 046700ca-7888-4405-b837-4b41076ab161
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5d5ddbfff676525c9282b9800e3ea40e') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 01:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:51:00' AND ends_at > '2026-10-06 01:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5d5ddbfff676525c9282b9800e3ea40e'), @cinestar_seed_movie_29, @cinestar_seed_room_22, '2026-10-06 01:10:00', '2026-10-06 02:51:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 10:15:00 +0700
-- source showtime_id: 08ac3dd5-387a-49c7-aef4-fae823a1b89b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cde2f56cbc5f5dc6993b48a394da7959') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 03:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:51:00' AND ends_at > '2026-10-06 03:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cde2f56cbc5f5dc6993b48a394da7959'), @cinestar_seed_movie_06, @cinestar_seed_room_22, '2026-10-06 03:15:00', '2026-10-06 04:51:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 12:15:00 +0700
-- source showtime_id: 03d50c0a-2927-46df-895f-fc1442323e1c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c20fa891653755e1a5fe9421f473023d') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 05:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:56:00' AND ends_at > '2026-10-06 05:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c20fa891653755e1a5fe9421f473023d'), @cinestar_seed_movie_29, @cinestar_seed_room_22, '2026-10-06 05:15:00', '2026-10-06 06:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 14:20:00 +0700
-- source showtime_id: 04448713-8be6-431f-9116-9a5b40e172ef
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a8228a977dc0561283cb46a0f160dca7') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:21:00' AND ends_at > '2026-10-06 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a8228a977dc0561283cb46a0f160dca7'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-06 07:20:00', '2026-10-06 09:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 16:50:00 +0700
-- source showtime_id: 643cd2f8-921e-410b-a84a-5211c49aa3ea
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('20186cfb5fca556c978144fba58c01a3') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:31:00' AND ends_at > '2026-10-06 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('20186cfb5fca556c978144fba58c01a3'), @cinestar_seed_movie_29, @cinestar_seed_room_22, '2026-10-06 09:50:00', '2026-10-06 11:31:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 18:50:00 +0700
-- source showtime_id: c23fd509-b57b-45f5-a525-755559103c08
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('34bd6e30676e539d84baf0c05520f832') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 11:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:45:00' AND ends_at > '2026-10-06 11:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('34bd6e30676e539d84baf0c05520f832'), @cinestar_seed_movie_04, @cinestar_seed_room_22, '2026-10-06 11:50:00', '2026-10-06 13:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 21:10:00 +0700
-- source showtime_id: a3c55974-c46a-4558-92ae-757e5236d56b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1694728dd7f35e6bafec025fd9f38d88') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 14:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:11:00' AND ends_at > '2026-10-06 14:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1694728dd7f35e6bafec025fd9f38d88'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-06 14:10:00', '2026-10-06 16:11:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Quốc Thanh (TP.HCM); room 06; local 2026-10-06 23:40:00 +0700
-- source showtime_id: 415dfe57-17a1-4ad1-be71-6488e8c9f864
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c3987a910714559a87fdecb31df649db') OR (room_id = @cinestar_seed_room_22 AND (starts_at = '2026-10-06 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:41:00' AND ends_at > '2026-10-06 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c3987a910714559a87fdecb31df649db'), @cinestar_seed_movie_14, @cinestar_seed_room_22, '2026-10-06 16:40:00', '2026-10-06 18:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_22 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 10:40:00 +0700
-- source showtime_id: 25c42261-7484-4071-b4ae-173429d2bf70
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7dad5b8f68035624bd92541d2677afaf') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 03:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:55:00' AND ends_at > '2026-10-05 03:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7dad5b8f68035624bd92541d2677afaf'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 03:40:00', '2026-10-05 05:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 13:20:00 +0700
-- source showtime_id: 3b34b0d7-8acd-49d7-81e5-a6de2cc925a7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('155975f55fd65be0b1e086c9922e43b5') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 06:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:35:00' AND ends_at > '2026-10-05 06:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('155975f55fd65be0b1e086c9922e43b5'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 06:20:00', '2026-10-05 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 16:00:00 +0700
-- source showtime_id: 4946c790-00ea-451c-ab1e-7a5f9f3486ae
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d1ba27123cc35261b6ca9ba9ef29bfb1') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:15:00' AND ends_at > '2026-10-05 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d1ba27123cc35261b6ca9ba9ef29bfb1'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 09:00:00', '2026-10-05 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 18:40:00 +0700
-- source showtime_id: d9b2ea7c-78db-4fb6-90e7-420ea4d0fdb8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('38a8ba7c12fd502088ad2ea470608e30') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:55:00' AND ends_at > '2026-10-05 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('38a8ba7c12fd502088ad2ea470608e30'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 11:40:00', '2026-10-05 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 21:20:00 +0700
-- source showtime_id: 9429ce7c-6a1b-47da-93b7-8be2cbbade5c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4f9b82618e155235a8d615480326cd80') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:35:00' AND ends_at > '2026-10-05 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4f9b82618e155235a8d615480326cd80'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 14:20:00', '2026-10-05 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-05 23:59:00 +0700
-- source showtime_id: d46d9c66-e2c6-49ee-aff0-710f07ff3ae5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6775b1cbf4625dbdab5fef78bd76cc56') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-05 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 19:14:00' AND ends_at > '2026-10-05 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6775b1cbf4625dbdab5fef78bd76cc56'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-05 16:59:00', '2026-10-05 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 20882b7c-bc18-4200-aad0-2e5e9569f262
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03fd2d9bd5885ea7b37dfedc75e024d7') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:15:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03fd2d9bd5885ea7b37dfedc75e024d7'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 01:00:00', '2026-10-06 03:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 10:40:00 +0700
-- source showtime_id: da3f4b2d-5297-496e-8436-68f61cb7efd6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3325b9d053275a35a8dd719eaecadda7') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 03:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:55:00' AND ends_at > '2026-10-06 03:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3325b9d053275a35a8dd719eaecadda7'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 03:40:00', '2026-10-06 05:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 13:20:00 +0700
-- source showtime_id: 480f251e-1d51-489d-837a-2c35a981c7bf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('dda96a99bc745ee4828ce660e942fbe9') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 06:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:35:00' AND ends_at > '2026-10-06 06:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('dda96a99bc745ee4828ce660e942fbe9'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 06:20:00', '2026-10-06 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 16:00:00 +0700
-- source showtime_id: 07211574-70fc-4e8f-a181-395036455c6a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4b1dfddbc19757349aff33266ed9c980') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:15:00' AND ends_at > '2026-10-06 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4b1dfddbc19757349aff33266ed9c980'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 09:00:00', '2026-10-06 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 18:40:00 +0700
-- source showtime_id: 81aef38f-28ea-4af0-a9e5-3f91cecdca7a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5e712fd9b1495eb394d018ae8e7696c6') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:55:00' AND ends_at > '2026-10-06 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5e712fd9b1495eb394d018ae8e7696c6'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 11:40:00', '2026-10-06 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 21:20:00 +0700
-- source showtime_id: 8f0cf871-9fb0-470d-b7bd-8afcdc12cc96
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('704c922b566a57568309d24cae547b33') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:35:00' AND ends_at > '2026-10-06 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('704c922b566a57568309d24cae547b33'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 14:20:00', '2026-10-06 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-06 23:59:00 +0700
-- source showtime_id: c1f28d39-d2be-4f81-ae58-666eb296ae27
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2bfb5a4e358059919832ea490d1b0831') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-06 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 19:14:00' AND ends_at > '2026-10-06 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2bfb5a4e358059919832ea490d1b0831'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-06 16:59:00', '2026-10-06 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 08:00:00 +0700
-- source showtime_id: d9cb88e5-bc20-4ab0-81c5-58b0ea540f81
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f31e6399a3605f22979ff6040abc6875') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:15:00' AND ends_at > '2026-10-07 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f31e6399a3605f22979ff6040abc6875'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 01:00:00', '2026-10-07 03:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 10:40:00 +0700
-- source showtime_id: d6601733-05ba-47ad-8604-95e5fa5f2780
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03ef629ee4935c81aa5ead4609d608a9') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 03:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 05:55:00' AND ends_at > '2026-10-07 03:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03ef629ee4935c81aa5ead4609d608a9'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 03:40:00', '2026-10-07 05:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 03:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 13:20:00 +0700
-- source showtime_id: 6c2e98a8-901e-449a-8d6b-00f14f45870b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ce813324ced55b2581b4eec3b16e5b11') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 06:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:35:00' AND ends_at > '2026-10-07 06:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ce813324ced55b2581b4eec3b16e5b11'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 06:20:00', '2026-10-07 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 16:00:00 +0700
-- source showtime_id: e86a9207-64d0-48da-b4d4-deba3e4ef054
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6a7b48e945ce57cebeeabdc95f272d50') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:15:00' AND ends_at > '2026-10-07 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6a7b48e945ce57cebeeabdc95f272d50'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 09:00:00', '2026-10-07 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 18:40:00 +0700
-- source showtime_id: 4239f8bf-25d5-44b4-9fa1-7a9b419a727a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8eca605e54fc5c1ca0bc102cb5bab6c1') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 11:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 13:55:00' AND ends_at > '2026-10-07 11:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8eca605e54fc5c1ca0bc102cb5bab6c1'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 11:40:00', '2026-10-07 13:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 11:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 21:20:00 +0700
-- source showtime_id: 5284695a-ac25-4f25-8a56-db64f12266c5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b1898c4600e35b9a9c87190bd5c6017a') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:35:00' AND ends_at > '2026-10-07 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b1898c4600e35b9a9c87190bd5c6017a'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 14:20:00', '2026-10-07 16:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-07 23:59:00 +0700
-- source showtime_id: da22de9c-e36e-41ee-87bc-0558597126df
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ddf20f880f89517489ecd8670d3c68d6') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-07 16:59:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 19:14:00' AND ends_at > '2026-10-07 16:59:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ddf20f880f89517489ecd8670d3c68d6'), @cinestar_seed_movie_15, @cinestar_seed_room_23, '2026-10-07 16:59:00', '2026-10-07 19:14:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:59:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÁN MẠNG XÉM HOÀN HẢO (T18); Cinestar Quốc Thanh (TP.HCM); room 05; local 2026-10-09 21:20:00 +0700
-- source showtime_id: 80713084-d182-4f2f-a342-a8c1052436a0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d19d2273c78d50b5b67b28699f8ec931') OR (room_id = @cinestar_seed_room_23 AND (starts_at = '2026-10-09 14:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-09 16:24:00' AND ends_at > '2026-10-09 14:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d19d2273c78d50b5b67b28699f8ec931'), @cinestar_seed_movie_10, @cinestar_seed_room_23, '2026-10-09 14:20:00', '2026-10-09 16:24:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_23 AND @cinestar_seed_movie_10 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-09 14:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-05 12:00:00 +0700
-- source showtime_id: 6b965775-7a95-460c-92d1-fb1030f16cce
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f1b7a7345bff5d3989149281d54d2b24') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-05 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:15:00' AND ends_at > '2026-10-05 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f1b7a7345bff5d3989149281d54d2b24'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-05 05:00:00', '2026-10-05 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-05 14:40:00 +0700
-- source showtime_id: 1050a0bf-1213-49eb-af61-c7c59f15eedf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1a3aafd402395081b6a668945f4225f3') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-05 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:55:00' AND ends_at > '2026-10-05 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1a3aafd402395081b6a668945f4225f3'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-05 07:40:00', '2026-10-05 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-05 17:20:00 +0700
-- source showtime_id: a6304542-db2f-4311-a1e9-197387ae1aae
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('42395edb867d5b549b57f82189215025') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-05 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:35:00' AND ends_at > '2026-10-05 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('42395edb867d5b549b57f82189215025'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-05 10:20:00', '2026-10-05 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-05 20:00:00 +0700
-- source showtime_id: 33077ee9-5e79-41a0-99b0-a259c10c7537
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d2588f6ef8525a3c9daf389ae54f25fc') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-05 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:15:00' AND ends_at > '2026-10-05 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d2588f6ef8525a3c9daf389ae54f25fc'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-05 13:00:00', '2026-10-05 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-05 22:40:00 +0700
-- source showtime_id: 232d8162-55f9-4c7d-8569-e74f987b8b0c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5bd60d0af69253778e0da10ae6d5a698') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:55:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5bd60d0af69253778e0da10ae6d5a698'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-05 15:40:00', '2026-10-05 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 09:20:00 +0700
-- source showtime_id: f2009172-1b96-418b-b678-827860557022
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b1cfe82bca2b52dfbd431253d9d5244a') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 02:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:35:00' AND ends_at > '2026-10-06 02:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b1cfe82bca2b52dfbd431253d9d5244a'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 02:20:00', '2026-10-06 04:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 12:00:00 +0700
-- source showtime_id: 3e1d7aed-fc31-4135-913a-86b8736aad9a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bfb96bf433a75917a0128076e4e6ea48') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:15:00' AND ends_at > '2026-10-06 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bfb96bf433a75917a0128076e4e6ea48'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 05:00:00', '2026-10-06 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 14:40:00 +0700
-- source showtime_id: c0f9a30c-5169-4336-8f99-d7f42d49f04c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('38b1fe1bbb055cd2ac564a9172a54f03') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:55:00' AND ends_at > '2026-10-06 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('38b1fe1bbb055cd2ac564a9172a54f03'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 07:40:00', '2026-10-06 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 17:20:00 +0700
-- source showtime_id: 5f2af136-c418-4dd2-ba3a-037e3e166357
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c4bcc18a0ed65b2abc87faa2ece75171') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:35:00' AND ends_at > '2026-10-06 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c4bcc18a0ed65b2abc87faa2ece75171'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 10:20:00', '2026-10-06 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 20:00:00 +0700
-- source showtime_id: 4494d56b-0ff8-48d5-a1e9-ea9021e58a69
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('458948ad0f1653f49578968e1523039c') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:15:00' AND ends_at > '2026-10-06 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('458948ad0f1653f49578968e1523039c'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 13:00:00', '2026-10-06 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-06 22:40:00 +0700
-- source showtime_id: cbc6332e-7f3f-4e41-bd55-b08a6572157f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f5a7ea6f5cdf5742bf034acfa354d4fd') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-06 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:55:00' AND ends_at > '2026-10-06 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f5a7ea6f5cdf5742bf034acfa354d4fd'), @cinestar_seed_movie_15, @cinestar_seed_room_24, '2026-10-06 15:40:00', '2026-10-06 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÁN MẠNG XÉM HOÀN HẢO (T18); Cinestar Quốc Thanh (TP.HCM); room 04; local 2026-10-09 21:00:00 +0700
-- source showtime_id: 6df42905-c79d-4b19-a5be-cc9029f67f46
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('465303c49455583c8c3df9370ac54e4c') OR (room_id = @cinestar_seed_room_24 AND (starts_at = '2026-10-09 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-09 16:04:00' AND ends_at > '2026-10-09 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('465303c49455583c8c3df9370ac54e4c'), @cinestar_seed_movie_10, @cinestar_seed_room_24, '2026-10-09 14:00:00', '2026-10-09 16:04:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_24 AND @cinestar_seed_movie_10 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-09 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-05 11:10:00 +0700
-- source showtime_id: ff2ac724-e197-43ef-a530-8c0900c7a510
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('66a14c5c8e93500d827f1f13b6062886') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-05 04:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:25:00' AND ends_at > '2026-10-05 04:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('66a14c5c8e93500d827f1f13b6062886'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-05 04:10:00', '2026-10-05 06:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-05 13:50:00 +0700
-- source showtime_id: d02430b9-854a-49db-831c-2b0dba790d48
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('21b522ee2f16576398a035159fd560c7') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-05 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:05:00' AND ends_at > '2026-10-05 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('21b522ee2f16576398a035159fd560c7'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-05 06:50:00', '2026-10-05 09:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-05 16:30:00 +0700
-- source showtime_id: 0eaa4781-7f0d-4dc2-95e7-ee9ea3bead3c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6762ccc485ac5da1a33a0627b40b37e3') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-05 09:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:45:00' AND ends_at > '2026-10-05 09:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6762ccc485ac5da1a33a0627b40b37e3'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-05 09:30:00', '2026-10-05 11:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-05 19:10:00 +0700
-- source showtime_id: 74851fb5-479c-4e0a-ab24-b6c5fde850f8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('454ee7898ab55a05863116bc7a054d65') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-05 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:25:00' AND ends_at > '2026-10-05 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('454ee7898ab55a05863116bc7a054d65'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-05 12:10:00', '2026-10-05 14:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-05 21:50:00 +0700
-- source showtime_id: 448defbb-6156-4ce6-8a59-2fc643d09737
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2b8622092861511c976389b087bc89ad') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-05 14:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:05:00' AND ends_at > '2026-10-05 14:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2b8622092861511c976389b087bc89ad'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-05 14:50:00', '2026-10-05 17:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 08:30:00 +0700
-- source showtime_id: 032bc840-8392-4ee6-b541-c7d8219677aa
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('abdc18c11a5d589e93c05ee58305edf4') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 01:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:45:00' AND ends_at > '2026-10-06 01:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('abdc18c11a5d589e93c05ee58305edf4'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 01:30:00', '2026-10-06 03:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 11:10:00 +0700
-- source showtime_id: 16789e23-5489-47ac-a3c1-7e6b6f2ccb05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9b97103dd6dc5e21bfc7a7e97c0e93a6') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 04:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:25:00' AND ends_at > '2026-10-06 04:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9b97103dd6dc5e21bfc7a7e97c0e93a6'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 04:10:00', '2026-10-06 06:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 13:50:00 +0700
-- source showtime_id: 3ff9a94b-4ae7-40f7-9d77-f2fce6cdbbd6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ea3db0a2b49a5b73bb7a453cd400708f') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:05:00' AND ends_at > '2026-10-06 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ea3db0a2b49a5b73bb7a453cd400708f'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 06:50:00', '2026-10-06 09:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 16:30:00 +0700
-- source showtime_id: a42d39ad-e13e-4b6a-9011-291c2d3b758f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('edc5682a6f3754ea8c66756b9b59626f') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 09:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:45:00' AND ends_at > '2026-10-06 09:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('edc5682a6f3754ea8c66756b9b59626f'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 09:30:00', '2026-10-06 11:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 19:10:00 +0700
-- source showtime_id: fd2261a2-e4c5-4385-9d64-21b375c9d11b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1be4f032ee1b566aab2b21c4bca0c642') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:25:00' AND ends_at > '2026-10-06 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1be4f032ee1b566aab2b21c4bca0c642'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 12:10:00', '2026-10-06 14:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 03; local 2026-10-06 21:50:00 +0700
-- source showtime_id: d5fb6a54-01d8-42ab-a0dc-e208b3f0674e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2fb14d47ff60594db89a859c113df938') OR (room_id = @cinestar_seed_room_25 AND (starts_at = '2026-10-06 14:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:05:00' AND ends_at > '2026-10-06 14:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2fb14d47ff60594db89a859c113df938'), @cinestar_seed_movie_15, @cinestar_seed_room_25, '2026-10-06 14:50:00', '2026-10-06 17:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_25 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-05 12:30:00 +0700
-- source showtime_id: 257b2d84-9c33-41cc-8a81-ad2f890bd055
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d52cda3b64c35834b31c2325474560fe') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-05 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:45:00' AND ends_at > '2026-10-05 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d52cda3b64c35834b31c2325474560fe'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-05 05:30:00', '2026-10-05 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-05 15:10:00 +0700
-- source showtime_id: 6a6f99ad-476f-4391-bc4d-6256f10278a0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8aa05a20768351cab010077e2946e0b3') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-05 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:25:00' AND ends_at > '2026-10-05 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8aa05a20768351cab010077e2946e0b3'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-05 08:10:00', '2026-10-05 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-05 17:50:00 +0700
-- source showtime_id: 15cffcce-000d-440a-8cbf-8a0b89e6a4a6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('635c08dd0e7c5f209caefdc174afdee9') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-05 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:05:00' AND ends_at > '2026-10-05 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('635c08dd0e7c5f209caefdc174afdee9'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-05 10:50:00', '2026-10-05 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-05 20:30:00 +0700
-- source showtime_id: 29c026cb-b915-40c6-812e-c13f3f5e9638
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b0932911bd8d5a758da5af13547a2b10') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-05 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:45:00' AND ends_at > '2026-10-05 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b0932911bd8d5a758da5af13547a2b10'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-05 13:30:00', '2026-10-05 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-05 23:10:00 +0700
-- source showtime_id: 2af66e0a-b3a5-40c0-a010-bccf0ee6b133
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('709b300e5d9e51089d4f2eba7cd0c73e') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-05 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:25:00' AND ends_at > '2026-10-05 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('709b300e5d9e51089d4f2eba7cd0c73e'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-05 16:10:00', '2026-10-05 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 09:50:00 +0700
-- source showtime_id: 4edc86ea-e12e-411e-8f3f-3f29af0462c1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('28971845a04652a4aff3924458547967') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:05:00' AND ends_at > '2026-10-06 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('28971845a04652a4aff3924458547967'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 02:50:00', '2026-10-06 05:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 12:30:00 +0700
-- source showtime_id: 7d0b24c8-402f-4222-9a36-0408da69fc8d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f2dce45b9a2e502692796805dfe16169') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:45:00' AND ends_at > '2026-10-06 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f2dce45b9a2e502692796805dfe16169'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 05:30:00', '2026-10-06 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 15:10:00 +0700
-- source showtime_id: 0da3bb6d-5f29-46b5-9401-080ba48c8f70
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6221f991460957c49563176d86175c85') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:25:00' AND ends_at > '2026-10-06 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6221f991460957c49563176d86175c85'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 08:10:00', '2026-10-06 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 17:50:00 +0700
-- source showtime_id: a307046a-89e7-4648-8ff4-d38a9f7d7588
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('24ba659e9c935793bed4d28705b2f828') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:05:00' AND ends_at > '2026-10-06 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('24ba659e9c935793bed4d28705b2f828'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 10:50:00', '2026-10-06 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 20:30:00 +0700
-- source showtime_id: 21cdd3ea-325c-42ed-830a-b94c63ff15b7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('90f41de25d5e5203a118b558251ea583') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:45:00' AND ends_at > '2026-10-06 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('90f41de25d5e5203a118b558251ea583'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 13:30:00', '2026-10-06 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Quốc Thanh (TP.HCM); room 01; local 2026-10-06 23:10:00 +0700
-- source showtime_id: 74da96d6-c168-4279-a00f-2c3dcd26b996
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f076bc73c950533aadc6c5c9de54c7e1') OR (room_id = @cinestar_seed_room_26 AND (starts_at = '2026-10-06 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:25:00' AND ends_at > '2026-10-06 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f076bc73c950533aadc6c5c9de54c7e1'), @cinestar_seed_movie_15, @cinestar_seed_room_26, '2026-10-06 16:10:00', '2026-10-06 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_26 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-05 11:20:00 +0700
-- source showtime_id: 4601f9d1-c1c1-4892-9856-4576aea553cb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('555409625a42577292d94bea08ba49df') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-05 04:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:35:00' AND ends_at > '2026-10-05 04:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('555409625a42577292d94bea08ba49df'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-05 04:20:00', '2026-10-05 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-05 14:00:00 +0700
-- source showtime_id: 05c5f20c-5ba6-4e8b-b5ea-a5395750cc16
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cb54df5db357503890978995059c7b39') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-05 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:15:00' AND ends_at > '2026-10-05 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cb54df5db357503890978995059c7b39'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-05 07:00:00', '2026-10-05 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-05 16:40:00 +0700
-- source showtime_id: c1c399da-2468-490e-b7e4-f2643ff1ce70
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a8214de1a8685a468e195163538a6c38') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-05 09:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:55:00' AND ends_at > '2026-10-05 09:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a8214de1a8685a468e195163538a6c38'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-05 09:40:00', '2026-10-05 11:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-05 19:20:00 +0700
-- source showtime_id: 845b7628-3a27-4d74-9643-728dac616ac0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6f84f3f4a3b55d128adcb77022af8774') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-05 12:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:35:00' AND ends_at > '2026-10-05 12:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6f84f3f4a3b55d128adcb77022af8774'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-05 12:20:00', '2026-10-05 14:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-05 22:20:00 +0700
-- source showtime_id: 64044f92-bff5-4ffd-aa8f-b9468d9e312e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f1b18033970251d4aa0cb18d9609b2b5') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-05 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:35:00' AND ends_at > '2026-10-05 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f1b18033970251d4aa0cb18d9609b2b5'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-05 15:20:00', '2026-10-05 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 08:10:00 +0700
-- source showtime_id: 7ea995ae-da0a-497d-b892-57706b82a4c0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d560356d94265894841d0f1c80852640') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 01:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:25:00' AND ends_at > '2026-10-06 01:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d560356d94265894841d0f1c80852640'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 01:10:00', '2026-10-06 03:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 10:50:00 +0700
-- source showtime_id: d3695a20-0b40-4cc5-95e9-ae6b3f2e5a4b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('76a95f9aae145267bc3e51bb22c39419') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 03:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:05:00' AND ends_at > '2026-10-06 03:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('76a95f9aae145267bc3e51bb22c39419'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 03:50:00', '2026-10-06 06:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 13:30:00 +0700
-- source showtime_id: 12016302-834a-49ec-a9e7-e66e45f625a2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('70f2c96702435ff6b6624c8dc4a8b4ad') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 06:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:45:00' AND ends_at > '2026-10-06 06:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('70f2c96702435ff6b6624c8dc4a8b4ad'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 06:30:00', '2026-10-06 08:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 16:10:00 +0700
-- source showtime_id: fd9a707a-65f4-40a1-82b9-3c97d04190d7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6fd6a3788b0d524bb50256f53322346c') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 09:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:25:00' AND ends_at > '2026-10-06 09:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6fd6a3788b0d524bb50256f53322346c'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 09:10:00', '2026-10-06 11:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 18:50:00 +0700
-- source showtime_id: cbb12a62-8741-4131-9fef-655eb2da9db8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d4321a37b38456e88ba015cd0a4b382b') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 11:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:05:00' AND ends_at > '2026-10-06 11:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d4321a37b38456e88ba015cd0a4b382b'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 11:50:00', '2026-10-06 14:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 06; local 2026-10-06 21:30:00 +0700
-- source showtime_id: ad16ca3d-c894-4a27-8da7-5cabca0d03d1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3f9689fdae2d5b7588b13e9a0701ad12') OR (room_id = @cinestar_seed_room_27 AND (starts_at = '2026-10-06 14:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:45:00' AND ends_at > '2026-10-06 14:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3f9689fdae2d5b7588b13e9a0701ad12'), @cinestar_seed_movie_15, @cinestar_seed_room_27, '2026-10-06 14:30:00', '2026-10-06 16:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_27 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-05 11:30:00 +0700
-- source showtime_id: 73eeb085-4d53-49af-94f1-55c94a9254f8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e85641de407c58dca19e94d58e2c8dc3') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-05 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:17:00' AND ends_at > '2026-10-05 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e85641de407c58dca19e94d58e2c8dc3'), @cinestar_seed_movie_13, @cinestar_seed_room_28, '2026-10-05 04:30:00', '2026-10-05 06:17:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-05 13:40:00 +0700
-- source showtime_id: 06733ba1-fc6f-4f20-aa6b-8750e2ba285f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('314b888f051a52ecbc9e7ddb593f01c5') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-05 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:35:00' AND ends_at > '2026-10-05 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('314b888f051a52ecbc9e7ddb593f01c5'), @cinestar_seed_movie_07, @cinestar_seed_room_28, '2026-10-05 06:40:00', '2026-10-05 08:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-05 16:00:00 +0700
-- source showtime_id: 6ea4c97e-8a58-465e-9006-1628e4cb9542
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cfd32a9225b25bc1b41c3a60a0460c7f') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-05 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:15:00' AND ends_at > '2026-10-05 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cfd32a9225b25bc1b41c3a60a0460c7f'), @cinestar_seed_movie_15, @cinestar_seed_room_28, '2026-10-05 09:00:00', '2026-10-05 11:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-05 18:45:00 +0700
-- source showtime_id: e3ee86e9-db3d-4167-80d6-f5a33fd04b4c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('95295b63940554eba18d8d799636c024') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-05 11:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:00:00' AND ends_at > '2026-10-05 11:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('95295b63940554eba18d8d799636c024'), @cinestar_seed_movie_15, @cinestar_seed_room_28, '2026-10-05 11:45:00', '2026-10-05 14:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-05 21:30:00 +0700
-- source showtime_id: afa1a78c-c168-43af-9a04-287a24b6f4e9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f69dbb59c1265f66bed87c9b409ce8d2') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-05 14:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:45:00' AND ends_at > '2026-10-05 14:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f69dbb59c1265f66bed87c9b409ce8d2'), @cinestar_seed_movie_15, @cinestar_seed_room_28, '2026-10-05 14:30:00', '2026-10-05 16:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 07:40:00 +0700
-- source showtime_id: eb99fd30-cd32-4ead-b143-c23d09827cf3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0e1aa2aa89fa5849bc5031e071d1e2be') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 00:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:00:00' AND ends_at > '2026-10-06 00:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0e1aa2aa89fa5849bc5031e071d1e2be'), @cinestar_seed_movie_08, @cinestar_seed_room_28, '2026-10-06 00:40:00', '2026-10-06 02:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 00:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 09:30:00 +0700
-- source showtime_id: 9bfce504-b055-4f9f-9257-d3a9ca162ef9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('dca39a6a2a8d5c4e9d82d1b2159c2b35') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 02:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:06:00' AND ends_at > '2026-10-06 02:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('dca39a6a2a8d5c4e9d82d1b2159c2b35'), @cinestar_seed_movie_06, @cinestar_seed_room_28, '2026-10-06 02:30:00', '2026-10-06 04:06:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 11:30:00 +0700
-- source showtime_id: 0d7dd6a3-8ec7-45f6-b269-69bd84160bde
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a14a267540695a7ab900dcbb84057567') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:25:00' AND ends_at > '2026-10-06 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a14a267540695a7ab900dcbb84057567'), @cinestar_seed_movie_07, @cinestar_seed_room_28, '2026-10-06 04:30:00', '2026-10-06 06:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 13:50:00 +0700
-- source showtime_id: 2690c093-2fa0-4e48-bd00-77e8c214c356
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4807cf8b764d5f22a8280bd32f1c4f62') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 06:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:37:00' AND ends_at > '2026-10-06 06:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4807cf8b764d5f22a8280bd32f1c4f62'), @cinestar_seed_movie_13, @cinestar_seed_room_28, '2026-10-06 06:50:00', '2026-10-06 08:37:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 16:00:00 +0700
-- source showtime_id: 4b371f85-e636-45a9-96dc-8bf70cede554
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('48b6fe7706105cf7986003882d45ad84') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 09:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:41:00' AND ends_at > '2026-10-06 09:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('48b6fe7706105cf7986003882d45ad84'), @cinestar_seed_movie_29, @cinestar_seed_room_28, '2026-10-06 09:00:00', '2026-10-06 10:41:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 18:05:00 +0700
-- source showtime_id: 9cf68779-4892-4e35-bfe2-f908ddfe7d05
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e5e466764c3f55a19a01968bc2bb88e2') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 11:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:00:00' AND ends_at > '2026-10-06 11:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e5e466764c3f55a19a01968bc2bb88e2'), @cinestar_seed_movie_07, @cinestar_seed_room_28, '2026-10-06 11:05:00', '2026-10-06 13:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 03; local 2026-10-06 20:30:00 +0700
-- source showtime_id: ab364777-0391-45ad-965c-5e048112b416
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fc3047cba4c155378dcdbbc50862871d') OR (room_id = @cinestar_seed_room_28 AND (starts_at = '2026-10-06 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:45:00' AND ends_at > '2026-10-06 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fc3047cba4c155378dcdbbc50862871d'), @cinestar_seed_movie_15, @cinestar_seed_room_28, '2026-10-06 13:30:00', '2026-10-06 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_28 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-05 12:00:00 +0700
-- source showtime_id: 0a92545e-20dc-4ef0-8a7d-489a01915a38
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('552bc55531a6569c8b741f2d4c433608') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-05 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:15:00' AND ends_at > '2026-10-05 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('552bc55531a6569c8b741f2d4c433608'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-05 05:00:00', '2026-10-05 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-05 14:40:00 +0700
-- source showtime_id: 1276e474-6e5c-483c-a6a4-3c1fcd94d709
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('eebd2133bc0f53e29d2c097139e51b64') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-05 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:55:00' AND ends_at > '2026-10-05 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('eebd2133bc0f53e29d2c097139e51b64'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-05 07:40:00', '2026-10-05 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-05 17:20:00 +0700
-- source showtime_id: fa114685-88b2-4d54-8c99-941f65333bd3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('eff2c1f794ec5b299ef419513715ef2c') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-05 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:35:00' AND ends_at > '2026-10-05 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('eff2c1f794ec5b299ef419513715ef2c'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-05 10:20:00', '2026-10-05 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-05 20:00:00 +0700
-- source showtime_id: c727a354-7a92-444d-99d8-33728179f49b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3d1f3b32264b5fe58a46c15c74d2d75c') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-05 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:15:00' AND ends_at > '2026-10-05 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3d1f3b32264b5fe58a46c15c74d2d75c'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-05 13:00:00', '2026-10-05 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-05 22:40:00 +0700
-- source showtime_id: d0dcc275-9ead-4d76-9dcc-e637d00e90ea
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7b5c66a093af5eef84c7bf8204865b3c') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:55:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7b5c66a093af5eef84c7bf8204865b3c'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-05 15:40:00', '2026-10-05 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 08:40:00 +0700
-- source showtime_id: a22be221-0e4d-4e08-9f36-b70965d49db4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('681dac59237f57c290882c801a5bc802') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 01:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:55:00' AND ends_at > '2026-10-06 01:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('681dac59237f57c290882c801a5bc802'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 01:40:00', '2026-10-06 03:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 11:20:00 +0700
-- source showtime_id: eeb91980-592d-4d50-8d90-6ce7ade30b2d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b8b41c67b8205788b07ddc27007254a8') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 04:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:35:00' AND ends_at > '2026-10-06 04:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b8b41c67b8205788b07ddc27007254a8'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 04:20:00', '2026-10-06 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 14:00:00 +0700
-- source showtime_id: 0dea9558-bdcc-41ca-b45e-1a5ab890f99f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d85b234e06065a3bb090cd1f260a0579') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:15:00' AND ends_at > '2026-10-06 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d85b234e06065a3bb090cd1f260a0579'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 07:00:00', '2026-10-06 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 16:40:00 +0700
-- source showtime_id: ef07f5a6-405e-4ce2-afe0-35ce2c612aa1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6b78cd8406025451aae1dbeb4bdb4ad6') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 09:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:55:00' AND ends_at > '2026-10-06 09:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6b78cd8406025451aae1dbeb4bdb4ad6'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 09:40:00', '2026-10-06 11:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 19:20:00 +0700
-- source showtime_id: 6d27d8e6-6eaf-44ce-b696-16cfbbd7afc2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('93f826754554528f8710885c530bd8b3') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 12:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:35:00' AND ends_at > '2026-10-06 12:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('93f826754554528f8710885c530bd8b3'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 12:20:00', '2026-10-06 14:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 01; local 2026-10-06 22:00:00 +0700
-- source showtime_id: d2fc864a-4b08-4e97-ad0d-8874380affa6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('811ac9fd67165db88110de83a9f6661b') OR (room_id = @cinestar_seed_room_29 AND (starts_at = '2026-10-06 15:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:15:00' AND ends_at > '2026-10-06 15:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('811ac9fd67165db88110de83a9f6661b'), @cinestar_seed_movie_15, @cinestar_seed_room_29, '2026-10-06 15:00:00', '2026-10-06 17:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_29 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-05 13:00:00 +0700
-- source showtime_id: 4271e119-9c6a-437c-b35b-e69787e6eb8d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cfa214d651b15fdc8fbc1383ba1fdede') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-05 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:15:00' AND ends_at > '2026-10-05 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cfa214d651b15fdc8fbc1383ba1fdede'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-05 06:00:00', '2026-10-05 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-05 15:40:00 +0700
-- source showtime_id: 4429bbb8-1bb3-427c-8c13-ee04669abf55
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2e5d55f8e89b55fbb00e3f0a1b9a4986') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-05 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:55:00' AND ends_at > '2026-10-05 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2e5d55f8e89b55fbb00e3f0a1b9a4986'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-05 08:40:00', '2026-10-05 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-05 18:20:00 +0700
-- source showtime_id: f3f3acfd-6b56-4bb0-acdc-073baf0b9723
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1b9f768e82db51179e6b635cde0c527b') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-05 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:35:00' AND ends_at > '2026-10-05 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1b9f768e82db51179e6b635cde0c527b'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-05 11:20:00', '2026-10-05 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-05 21:00:00 +0700
-- source showtime_id: d875bff7-a78e-4a41-bacc-f086720ac4ff
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d203e17eabf356a3acb0cb60f5dad642') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-05 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:15:00' AND ends_at > '2026-10-05 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d203e17eabf356a3acb0cb60f5dad642'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-05 14:00:00', '2026-10-05 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 07:50:00 +0700
-- source showtime_id: ed49e62e-c56b-4756-82aa-d04b928357bd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d1d49bd2d62358a1a2d44b5b31c46465') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 00:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:52:00' AND ends_at > '2026-10-06 00:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d1d49bd2d62358a1a2d44b5b31c46465'), @cinestar_seed_movie_05, @cinestar_seed_room_30, '2026-10-06 00:50:00', '2026-10-06 02:52:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 00:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 10:20:00 +0700
-- source showtime_id: 124e729b-f8ab-4782-a746-e2fc66d79d6f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d5a5fd6af4c75120b651e65ffc33771d') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 03:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:35:00' AND ends_at > '2026-10-06 03:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d5a5fd6af4c75120b651e65ffc33771d'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-06 03:20:00', '2026-10-06 05:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 13:00:00 +0700
-- source showtime_id: 560bac93-b049-4463-9a0e-ca88956d1c81
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3c4a571c34a55279933b91bafa57f974') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:15:00' AND ends_at > '2026-10-06 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3c4a571c34a55279933b91bafa57f974'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-06 06:00:00', '2026-10-06 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 15:40:00 +0700
-- source showtime_id: 93bd2130-2e5a-47a1-8fe5-1bae58302e50
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7073cfda247d5503a2b9f0aa19d2fee1') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:55:00' AND ends_at > '2026-10-06 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7073cfda247d5503a2b9f0aa19d2fee1'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-06 08:40:00', '2026-10-06 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 18:20:00 +0700
-- source showtime_id: a296327f-829b-42b4-b370-75958bdbdea1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f2bb398e0e3251ba8790b28952b5a7a3') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:35:00' AND ends_at > '2026-10-06 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f2bb398e0e3251ba8790b28952b5a7a3'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-06 11:20:00', '2026-10-06 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 02; local 2026-10-06 21:00:00 +0700
-- source showtime_id: e964e7b8-0f63-48e6-b4c9-30f0af8a5a86
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('99c0346f4a425630b2dbcf2b76b620b0') OR (room_id = @cinestar_seed_room_30 AND (starts_at = '2026-10-06 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:15:00' AND ends_at > '2026-10-06 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('99c0346f4a425630b2dbcf2b76b620b0'), @cinestar_seed_movie_15, @cinestar_seed_room_30, '2026-10-06 14:00:00', '2026-10-06 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_30 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-05 10:50:00 +0700
-- source showtime_id: 6f3fe9ac-8597-4625-9f9a-687e57197172
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('03ecdfb64a18506199585338d52e2fc6') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-05 03:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:05:00' AND ends_at > '2026-10-05 03:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('03ecdfb64a18506199585338d52e2fc6'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-05 03:50:00', '2026-10-05 06:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-05 13:30:00 +0700
-- source showtime_id: 9e6bd525-7389-40a7-b229-9d77ef53fa1d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('65e61b9d603c5ad49f16a6e0121465f5') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-05 06:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:45:00' AND ends_at > '2026-10-05 06:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('65e61b9d603c5ad49f16a6e0121465f5'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-05 06:30:00', '2026-10-05 08:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-05 16:15:00 +0700
-- source showtime_id: d81ef5ed-9bbc-439b-ada2-18b88afb8019
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1c270aa264eb51ec9d99026dd9fb585e') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-05 09:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:30:00' AND ends_at > '2026-10-05 09:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1c270aa264eb51ec9d99026dd9fb585e'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-05 09:15:00', '2026-10-05 11:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-05 19:00:00 +0700
-- source showtime_id: 867826a9-d455-4983-af7c-b58e2d8f4e6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e764da30387551b887fec882a4589a90') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-05 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:15:00' AND ends_at > '2026-10-05 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e764da30387551b887fec882a4589a90'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-05 12:00:00', '2026-10-05 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-05 22:00:00 +0700
-- source showtime_id: 897a2fb9-239a-4719-9cdc-c0b28e0e6f97
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ff686cdb0c1b5462ac16e5d306c9a68d') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-05 15:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:15:00' AND ends_at > '2026-10-05 15:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ff686cdb0c1b5462ac16e5d306c9a68d'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-05 15:00:00', '2026-10-05 17:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 09:10:00 +0700
-- source showtime_id: cc9d67c6-9e6a-4975-bb57-ee042ef4cd7c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b5f89ecb18205e3586c7fa91139221a8') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 02:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:25:00' AND ends_at > '2026-10-06 02:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b5f89ecb18205e3586c7fa91139221a8'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 02:10:00', '2026-10-06 04:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 11:50:00 +0700
-- source showtime_id: 95ff9083-3792-4c84-9501-544a66f3b9c2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5d8cfea5e9c1570a86f71358907b2b24') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:05:00' AND ends_at > '2026-10-06 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5d8cfea5e9c1570a86f71358907b2b24'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 04:50:00', '2026-10-06 07:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 14:30:00 +0700
-- source showtime_id: 6916e3ce-a1ee-42c8-b30b-30fab818cca1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('14c39d89e2e750f1af0acda272ffb415') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 07:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:45:00' AND ends_at > '2026-10-06 07:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('14c39d89e2e750f1af0acda272ffb415'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 07:30:00', '2026-10-06 09:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 17:10:00 +0700
-- source showtime_id: f6bb0916-5789-4d68-ad4e-2e5a3032a37a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('69a5e164ed7b548da2ffa35f2d0b0cb7') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 10:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:25:00' AND ends_at > '2026-10-06 10:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('69a5e164ed7b548da2ffa35f2d0b0cb7'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 10:10:00', '2026-10-06 12:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 19:50:00 +0700
-- source showtime_id: a28a2e98-4b94-485e-88fd-273113d6f398
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8209f78a7dce567f84c3add1aa78957e') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 12:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:05:00' AND ends_at > '2026-10-06 12:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8209f78a7dce567f84c3add1aa78957e'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 12:50:00', '2026-10-06 15:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Mỹ Tho (Đồng Tháp); room 05; local 2026-10-06 22:30:00 +0700
-- source showtime_id: 99cc6082-19f1-4f07-a8b0-62acd4bf35c5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('515ea624ea1c5cb581e1b712142a138d') OR (room_id = @cinestar_seed_room_31 AND (starts_at = '2026-10-06 15:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:45:00' AND ends_at > '2026-10-06 15:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('515ea624ea1c5cb581e1b712142a138d'), @cinestar_seed_movie_15, @cinestar_seed_room_31, '2026-10-06 15:30:00', '2026-10-06 17:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_31 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-05 12:10:00 +0700
-- source showtime_id: 4a30e824-8b94-40d6-b863-10334f7debd8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2e6fc0ac19a8503b88565ce8ab1e7568') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-05 05:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:25:00' AND ends_at > '2026-10-05 05:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2e6fc0ac19a8503b88565ce8ab1e7568'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-05 05:10:00', '2026-10-05 07:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-05 14:50:00 +0700
-- source showtime_id: c4654cd2-0e0a-47f6-b908-77a3f1d1df3d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7e32073fef915a1a960fe3598b0f75c0') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-05 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:05:00' AND ends_at > '2026-10-05 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7e32073fef915a1a960fe3598b0f75c0'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-05 07:50:00', '2026-10-05 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-05 17:30:00 +0700
-- source showtime_id: e2a501b8-485e-4307-af0c-d408c32c8fd2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c20300c47c3a569fbaeb6ec1888a0de9') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-05 10:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:45:00' AND ends_at > '2026-10-05 10:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c20300c47c3a569fbaeb6ec1888a0de9'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-05 10:30:00', '2026-10-05 12:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-05 20:10:00 +0700
-- source showtime_id: a4f9d528-17de-4659-8b37-b96cd35a78a3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('43d8ee87c4ed598192711c91df191de1') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-05 13:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:25:00' AND ends_at > '2026-10-05 13:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('43d8ee87c4ed598192711c91df191de1'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-05 13:10:00', '2026-10-05 15:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-05 22:50:00 +0700
-- source showtime_id: 58334a14-8a01-4863-8923-5053103bdebe
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6b1bff574dbb5c9497c10c233e8567f1') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-05 15:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:05:00' AND ends_at > '2026-10-05 15:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6b1bff574dbb5c9497c10c233e8567f1'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-05 15:50:00', '2026-10-05 18:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 174f001e-4065-464e-958e-c8ef48f033ba
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('780a674c577e58a39b0247308933fe33') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:20:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('780a674c577e58a39b0247308933fe33'), @cinestar_seed_movie_08, @cinestar_seed_room_32, '2026-10-06 01:00:00', '2026-10-06 02:20:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 09:40:00 +0700
-- source showtime_id: 8cf53540-d9d8-4b3f-9ebb-096d59d2809e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f796655950f65da8972d35ddb7f08dec') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 02:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:42:00' AND ends_at > '2026-10-06 02:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f796655950f65da8972d35ddb7f08dec'), @cinestar_seed_movie_05, @cinestar_seed_room_32, '2026-10-06 02:40:00', '2026-10-06 04:42:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 12:10:00 +0700
-- source showtime_id: 0c7db0b6-c04b-4b95-9688-920ff36d306d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b579bb161321566f87e0c87c965d3f8d') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 05:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:25:00' AND ends_at > '2026-10-06 05:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b579bb161321566f87e0c87c965d3f8d'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-06 05:10:00', '2026-10-06 07:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 14:50:00 +0700
-- source showtime_id: e3e771ea-c252-456d-adb2-5b3073c94c72
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d6eee8266a195b3c9b79e6dc54f5cb94') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:05:00' AND ends_at > '2026-10-06 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d6eee8266a195b3c9b79e6dc54f5cb94'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-06 07:50:00', '2026-10-06 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 17:30:00 +0700
-- source showtime_id: 133a1d31-48ca-4041-b7f6-7f5fefcf8843
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b951f5e27aea5242afd269a634877bc8') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 10:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:45:00' AND ends_at > '2026-10-06 10:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b951f5e27aea5242afd269a634877bc8'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-06 10:30:00', '2026-10-06 12:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 20:10:00 +0700
-- source showtime_id: 9dee0c15-68ad-4eee-9ffb-bbcb29673006
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('46ef340286c55ca5b21bad5782558e47') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 13:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:25:00' AND ends_at > '2026-10-06 13:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('46ef340286c55ca5b21bad5782558e47'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-06 13:10:00', '2026-10-06 15:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-06 22:50:00 +0700
-- source showtime_id: f74d3e24-305c-45da-b0ca-a8beeede35dc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('15b6089b74465a49b28d4d755ce41d5e') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-06 15:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:05:00' AND ends_at > '2026-10-06 15:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('15b6089b74465a49b28d4d755ce41d5e'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-06 15:50:00', '2026-10-06 18:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-07 12:10:00 +0700
-- source showtime_id: ca6f147b-e9de-4366-9b46-e8c721d9946c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('099210d778d35841b07a17aec4e9c3a0') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-07 05:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:25:00' AND ends_at > '2026-10-07 05:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('099210d778d35841b07a17aec4e9c3a0'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-07 05:10:00', '2026-10-07 07:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-07 14:50:00 +0700
-- source showtime_id: d69adc8b-f581-439b-b42d-52b8bb6fc066
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f29460e69ffe58f192d19e44cca79bab') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-07 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:05:00' AND ends_at > '2026-10-07 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f29460e69ffe58f192d19e44cca79bab'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-07 07:50:00', '2026-10-07 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-07 17:30:00 +0700
-- source showtime_id: 961c2ddf-8ff7-4a5f-9e72-ad560d81fb6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e7d7aa938eb2531792e05c18235f1f3a') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-07 10:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:45:00' AND ends_at > '2026-10-07 10:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e7d7aa938eb2531792e05c18235f1f3a'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-07 10:30:00', '2026-10-07 12:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-07 20:10:00 +0700
-- source showtime_id: aeae65ea-0dca-4fd6-a861-87dd11b8091b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b219facd2a925e90bfb6772f3898362f') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-07 13:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:25:00' AND ends_at > '2026-10-07 13:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b219facd2a925e90bfb6772f3898362f'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-07 13:10:00', '2026-10-07 15:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 01; local 2026-10-07 22:50:00 +0700
-- source showtime_id: 87a9acb6-52ee-4452-924a-66f2c9363034
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4d6ff5ebd99356d4972624542b85c8cf') OR (room_id = @cinestar_seed_room_32 AND (starts_at = '2026-10-07 15:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 18:05:00' AND ends_at > '2026-10-07 15:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4d6ff5ebd99356d4972624542b85c8cf'), @cinestar_seed_movie_15, @cinestar_seed_room_32, '2026-10-07 15:50:00', '2026-10-07 18:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_32 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-05 11:50:00 +0700
-- source showtime_id: a1b1c1d1-b80e-41be-be6d-8dbbe30d0c24
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4a612bf131565bbd8ef473b9c75c77f8') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-05 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:53:00' AND ends_at > '2026-10-05 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4a612bf131565bbd8ef473b9c75c77f8'), @cinestar_seed_movie_20, @cinestar_seed_room_33, '2026-10-05 04:50:00', '2026-10-05 07:53:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-05 15:20:00 +0700
-- source showtime_id: dfa40095-3485-405a-b0eb-9d13824e4b6c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7d4463cd3559539b84f8303a507f4d52') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-05 08:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:35:00' AND ends_at > '2026-10-05 08:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7d4463cd3559539b84f8303a507f4d52'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-05 08:20:00', '2026-10-05 10:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-05 18:00:00 +0700
-- source showtime_id: bf6ae01d-e08c-42b0-ab3b-3243f038775e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('989dd9fbea4a5c0d8b21de57a40ed4dd') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-05 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:15:00' AND ends_at > '2026-10-05 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('989dd9fbea4a5c0d8b21de57a40ed4dd'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-05 11:00:00', '2026-10-05 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-05 20:40:00 +0700
-- source showtime_id: 117b073c-a42b-42d6-a73c-d62791a430d5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4d570565b1cf536d87571884567df055') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-05 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:55:00' AND ends_at > '2026-10-05 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4d570565b1cf536d87571884567df055'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-05 13:40:00', '2026-10-05 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-05 23:20:00 +0700
-- source showtime_id: 366979fe-f763-4951-8779-4102510e7e58
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fd9a8563abe053439a53b28280d2c66e') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-05 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:35:00' AND ends_at > '2026-10-05 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fd9a8563abe053439a53b28280d2c66e'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-05 16:20:00', '2026-10-05 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 09:50:00 +0700
-- source showtime_id: 1675d6e3-df65-486b-957b-0955b73ecf72
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('42d02edb464d513ca7c0b8bf95bbc981') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:31:00' AND ends_at > '2026-10-06 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('42d02edb464d513ca7c0b8bf95bbc981'), @cinestar_seed_movie_29, @cinestar_seed_room_33, '2026-10-06 02:50:00', '2026-10-06 04:31:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 11:50:00 +0700
-- source showtime_id: 93f4e74f-1651-4744-a7ad-0ae4624b1668
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('47ed7046461c568ea54d8b468ca27341') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 04:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:53:00' AND ends_at > '2026-10-06 04:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('47ed7046461c568ea54d8b468ca27341'), @cinestar_seed_movie_20, @cinestar_seed_room_33, '2026-10-06 04:50:00', '2026-10-06 07:53:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 15:20:00 +0700
-- source showtime_id: 402a079e-fba1-43a5-9c8f-85d9d6bf7180
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('06501b82d79e500da9b2555ca3a43bf0') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 08:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:35:00' AND ends_at > '2026-10-06 08:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('06501b82d79e500da9b2555ca3a43bf0'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-06 08:20:00', '2026-10-06 10:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 18:00:00 +0700
-- source showtime_id: 09bd8a30-4711-4491-a72b-b7e63d0843ec
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ff04fbad38495e388fa2f610148a1d4c') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 11:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:15:00' AND ends_at > '2026-10-06 11:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ff04fbad38495e388fa2f610148a1d4c'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-06 11:00:00', '2026-10-06 13:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 20:40:00 +0700
-- source showtime_id: f5c8b3c0-1bf8-46ee-949d-3bf53a52b5ab
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('db9e5ee51c985082801e9f3ee3c016fe') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 13:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:55:00' AND ends_at > '2026-10-06 13:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('db9e5ee51c985082801e9f3ee3c016fe'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-06 13:40:00', '2026-10-06 15:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 02; local 2026-10-06 23:20:00 +0700
-- source showtime_id: 1982ed2e-86e7-4141-b217-5505b7fbc12b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1737c3e99dce502490f3bafb3b7a7524') OR (room_id = @cinestar_seed_room_33 AND (starts_at = '2026-10-06 16:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:35:00' AND ends_at > '2026-10-06 16:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1737c3e99dce502490f3bafb3b7a7524'), @cinestar_seed_movie_15, @cinestar_seed_room_33, '2026-10-06 16:20:00', '2026-10-06 18:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_33 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 10:30:00 +0700
-- source showtime_id: 3245b5d8-af0a-4425-930a-d36f2e61e803
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2d2d48dde00b5afc94087f16873d9cd1') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 03:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:45:00' AND ends_at > '2026-10-05 03:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2d2d48dde00b5afc94087f16873d9cd1'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 03:30:00', '2026-10-05 05:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 13:10:00 +0700
-- source showtime_id: 0e23d0a6-243c-4685-bd09-f6fa1137c881
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7e7e84dcb0aa5939b66678c68d37aefc') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 06:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:25:00' AND ends_at > '2026-10-05 06:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7e7e84dcb0aa5939b66678c68d37aefc'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 06:10:00', '2026-10-05 08:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 15:50:00 +0700
-- source showtime_id: 1d7fa792-b523-48a0-90cb-cce8a08879fc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('35f612db866658d288ea80ddf949cf8e') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 08:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:05:00' AND ends_at > '2026-10-05 08:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('35f612db866658d288ea80ddf949cf8e'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 08:50:00', '2026-10-05 11:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 18:30:00 +0700
-- source showtime_id: ad6e45da-6a42-4320-a080-3f4c51251690
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6d534778dd445d35803aa6b9584b2210') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 11:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:45:00' AND ends_at > '2026-10-05 11:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6d534778dd445d35803aa6b9584b2210'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 11:30:00', '2026-10-05 13:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 21:10:00 +0700
-- source showtime_id: ce6869e4-88a4-4447-b2dd-68c73c73c325
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1b5e01af73015adf826d8a4373d81989') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 14:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:25:00' AND ends_at > '2026-10-05 14:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1b5e01af73015adf826d8a4373d81989'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 14:10:00', '2026-10-05 16:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-05 23:50:00 +0700
-- source showtime_id: 7911a297-b6de-43c6-8c3c-be6568d3fa01
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ec78838e44ea5e4eac61b858f26be35b') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-05 16:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 19:05:00' AND ends_at > '2026-10-05 16:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ec78838e44ea5e4eac61b858f26be35b'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-05 16:50:00', '2026-10-05 19:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 08:10:00 +0700
-- source showtime_id: af1b5f25-b2fc-40c9-93f9-c5be46ceb839
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1568e398339a5376a29341fa10c5a631') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 01:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:05:00' AND ends_at > '2026-10-06 01:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1568e398339a5376a29341fa10c5a631'), @cinestar_seed_movie_04, @cinestar_seed_room_34, '2026-10-06 01:10:00', '2026-10-06 03:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 10:30:00 +0700
-- source showtime_id: db87b162-208a-461f-b050-2fd2571a7698
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9d4c1d7113e1506eb2730a7728c1d10f') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 03:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:45:00' AND ends_at > '2026-10-06 03:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9d4c1d7113e1506eb2730a7728c1d10f'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 03:30:00', '2026-10-06 05:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 13:10:00 +0700
-- source showtime_id: 13aa316c-055f-4b7a-8c9d-c389fbb3520e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('19097d8760dd54188dfc341a24f68dca') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 06:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:25:00' AND ends_at > '2026-10-06 06:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('19097d8760dd54188dfc341a24f68dca'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 06:10:00', '2026-10-06 08:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 15:50:00 +0700
-- source showtime_id: 8ee8b831-3bb0-4637-8619-224631bbb91c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('44be88fa377859a684e12123e642761c') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 08:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:05:00' AND ends_at > '2026-10-06 08:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('44be88fa377859a684e12123e642761c'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 08:50:00', '2026-10-06 11:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 18:30:00 +0700
-- source showtime_id: 0617edb3-a602-4681-9690-92fa6cdd62c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('172bcbf718b45a7ab96ec3266c604e6c') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 11:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:45:00' AND ends_at > '2026-10-06 11:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('172bcbf718b45a7ab96ec3266c604e6c'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 11:30:00', '2026-10-06 13:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 21:10:00 +0700
-- source showtime_id: e4a836b1-cd66-4dfa-bef8-3b9d4a088091
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('14b18cd825945dc3908e66ed4a59a574') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 14:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:25:00' AND ends_at > '2026-10-06 14:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('14b18cd825945dc3908e66ed4a59a574'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 14:10:00', '2026-10-06 16:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-06 23:50:00 +0700
-- source showtime_id: 4afb0cb2-bbcf-4f31-92ef-1c54345eefef
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('830580bc5f6e5efc9dc3f399afd3b9f8') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-06 16:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 19:05:00' AND ends_at > '2026-10-06 16:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('830580bc5f6e5efc9dc3f399afd3b9f8'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-06 16:50:00', '2026-10-06 19:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 10:30:00 +0700
-- source showtime_id: 081a2e14-3be6-4a59-af13-c8b7275ed770
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('804d7648d3185d74ad0e950f0709d36e') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 03:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 05:45:00' AND ends_at > '2026-10-07 03:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('804d7648d3185d74ad0e950f0709d36e'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 03:30:00', '2026-10-07 05:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 03:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 13:10:00 +0700
-- source showtime_id: 0d37cc14-da29-49ab-aa9a-294460cf5ec1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b0ac6073d6c45e38878e543c05538488') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 06:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:25:00' AND ends_at > '2026-10-07 06:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b0ac6073d6c45e38878e543c05538488'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 06:10:00', '2026-10-07 08:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 15:50:00 +0700
-- source showtime_id: f9b70bb0-c5ee-462b-8d95-f760a1aac1d5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e35766b8e104554fbd659d4b28e5688b') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 08:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:05:00' AND ends_at > '2026-10-07 08:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e35766b8e104554fbd659d4b28e5688b'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 08:50:00', '2026-10-07 11:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 18:30:00 +0700
-- source showtime_id: 7503a780-58da-4162-b93d-10bb302489df
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a3a0bff03e925953854eb94ca4ec1178') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 11:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 13:45:00' AND ends_at > '2026-10-07 11:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a3a0bff03e925953854eb94ca4ec1178'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 11:30:00', '2026-10-07 13:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 11:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 21:10:00 +0700
-- source showtime_id: a77a66c9-64e4-44f2-b823-b82d0e9cd7b1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e8084dbbf8f3513a998db3a99fed6109') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 14:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:25:00' AND ends_at > '2026-10-07 14:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e8084dbbf8f3513a998db3a99fed6109'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 14:10:00', '2026-10-07 16:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 03; local 2026-10-07 23:50:00 +0700
-- source showtime_id: bd55afb2-0870-4967-a242-cf67dcf8004e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6af03867407d52c08900414d73a59269') OR (room_id = @cinestar_seed_room_34 AND (starts_at = '2026-10-07 16:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 19:05:00' AND ends_at > '2026-10-07 16:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6af03867407d52c08900414d73a59269'), @cinestar_seed_movie_15, @cinestar_seed_room_34, '2026-10-07 16:50:00', '2026-10-07 19:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_34 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-05 11:40:00 +0700
-- source showtime_id: f9f94329-3ed9-45ce-83d3-15499b118147
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('379f83ca748250878ccc79e6b04515ab') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-05 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:55:00' AND ends_at > '2026-10-05 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('379f83ca748250878ccc79e6b04515ab'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-05 04:40:00', '2026-10-05 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-05 14:20:00 +0700
-- source showtime_id: 2d8f1c6d-f1bb-4d2b-b257-7bb08bc6a2de
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b70a4a1411bf5c1fbc9ff71e0299f166') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-05 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:35:00' AND ends_at > '2026-10-05 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b70a4a1411bf5c1fbc9ff71e0299f166'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-05 07:20:00', '2026-10-05 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-05 17:00:00 +0700
-- source showtime_id: 22b13abc-1efc-4c32-a451-b5e0ce28580a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2b8b09de17b05bab9886f416b29169f4') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-05 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:15:00' AND ends_at > '2026-10-05 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2b8b09de17b05bab9886f416b29169f4'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-05 10:00:00', '2026-10-05 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-05 19:40:00 +0700
-- source showtime_id: 7df0a9fe-ae57-4918-bbf5-fd79f03de62a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('767a5793f82e584e80a9c7841cf78ffc') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-05 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:55:00' AND ends_at > '2026-10-05 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('767a5793f82e584e80a9c7841cf78ffc'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-05 12:40:00', '2026-10-05 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-05 22:20:00 +0700
-- source showtime_id: 5f3d19d7-e1db-4737-82f2-8ad70b951677
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ffea4cac877f5628bd9db1bd166449a9') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-05 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:35:00' AND ends_at > '2026-10-05 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ffea4cac877f5628bd9db1bd166449a9'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-05 15:20:00', '2026-10-05 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 09:00:00 +0700
-- source showtime_id: 6312ea94-68b8-423e-9778-6bbfa2419bb0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e5b141608829524fad24b8d538771384') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:15:00' AND ends_at > '2026-10-06 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e5b141608829524fad24b8d538771384'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 02:00:00', '2026-10-06 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 11:40:00 +0700
-- source showtime_id: 5662878d-d909-42da-8374-768cce3fd673
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9c71ed8a5c935f3885b45f3f199a83b0') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:55:00' AND ends_at > '2026-10-06 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9c71ed8a5c935f3885b45f3f199a83b0'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 04:40:00', '2026-10-06 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 14:20:00 +0700
-- source showtime_id: 862733c0-504e-452e-96ed-7b71d430ebf4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2f9f6f1eed5f5828babccffd6edb871c') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:35:00' AND ends_at > '2026-10-06 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2f9f6f1eed5f5828babccffd6edb871c'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 07:20:00', '2026-10-06 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 17:00:00 +0700
-- source showtime_id: fbc963df-82eb-495e-af74-8a1f4ca00be9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3c67fc4d2cd552a8be93e90421b0a224') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:15:00' AND ends_at > '2026-10-06 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3c67fc4d2cd552a8be93e90421b0a224'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 10:00:00', '2026-10-06 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 19:40:00 +0700
-- source showtime_id: 8cc7db1c-6289-4657-beaf-2766e3fb42a5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('22ed9c44a9be5de591e0a68813cba1cc') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:55:00' AND ends_at > '2026-10-06 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('22ed9c44a9be5de591e0a68813cba1cc'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 12:40:00', '2026-10-06 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-06 22:20:00 +0700
-- source showtime_id: a6669a4a-dabf-4771-bfcc-31fe32ea66e5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('913b069b79195275bef850af4d23e5c2') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-06 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:35:00' AND ends_at > '2026-10-06 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('913b069b79195275bef850af4d23e5c2'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-06 15:20:00', '2026-10-06 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 09:00:00 +0700
-- source showtime_id: 78890b44-d4aa-4756-8c1b-5e6203aced21
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b4efbcecdd04534193de2ddd58be56a0') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:15:00' AND ends_at > '2026-10-07 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b4efbcecdd04534193de2ddd58be56a0'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 02:00:00', '2026-10-07 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 11:40:00 +0700
-- source showtime_id: a07596b4-bf47-456f-bb8c-d0ead115911b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4b85c63814b1515eac65f40e8b5a2137') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:55:00' AND ends_at > '2026-10-07 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4b85c63814b1515eac65f40e8b5a2137'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 04:40:00', '2026-10-07 06:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 14:20:00 +0700
-- source showtime_id: 2af60d95-5a26-4564-9f0b-776c70d9fad3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f07403ea5191548e972e834b542f38ad') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:35:00' AND ends_at > '2026-10-07 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f07403ea5191548e972e834b542f38ad'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 07:20:00', '2026-10-07 09:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 17:00:00 +0700
-- source showtime_id: 0f34fb43-9075-4597-ab74-65915a67998e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('efd6aaa6b2885ede9cf193371d0e0aae') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:15:00' AND ends_at > '2026-10-07 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('efd6aaa6b2885ede9cf193371d0e0aae'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 10:00:00', '2026-10-07 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 19:40:00 +0700
-- source showtime_id: a0a161ca-df65-45be-bdf4-f303a0f87611
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d0d3f7a1e0f8599db0aa2dd810903d76') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:55:00' AND ends_at > '2026-10-07 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d0d3f7a1e0f8599db0aa2dd810903d76'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 12:40:00', '2026-10-07 14:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Sinh Viên (TP.HCM); room 05; local 2026-10-07 22:20:00 +0700
-- source showtime_id: 39a88f7f-e336-4d2b-908b-e388a1a96928
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3efe6c64fc51534ca759078868591e30') OR (room_id = @cinestar_seed_room_35 AND (starts_at = '2026-10-07 15:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 17:35:00' AND ends_at > '2026-10-07 15:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3efe6c64fc51534ca759078868591e30'), @cinestar_seed_movie_15, @cinestar_seed_room_35, '2026-10-07 15:20:00', '2026-10-07 17:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_35 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-05 14:00:00 +0700
-- source showtime_id: 4b44bc54-687a-4dbf-a34a-92b3e99e6064
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4accf519bd2f577dad71ba1bd091ebd6') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-05 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:15:00' AND ends_at > '2026-10-05 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4accf519bd2f577dad71ba1bd091ebd6'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-05 07:00:00', '2026-10-05 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-05 16:45:00 +0700
-- source showtime_id: 146d2c7e-fdf0-424b-b92b-6fbf5ae2f879
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('76ec739e2fb45df3aabac9e0e9aba9de') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-05 09:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:00:00' AND ends_at > '2026-10-05 09:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('76ec739e2fb45df3aabac9e0e9aba9de'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-05 09:45:00', '2026-10-05 12:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-05 19:30:00 +0700
-- source showtime_id: 40240540-8339-4dd7-a980-0d8ba0254af4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('47f3fdf4afc45907b21f40615626efd8') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-05 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:45:00' AND ends_at > '2026-10-05 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('47f3fdf4afc45907b21f40615626efd8'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-05 12:30:00', '2026-10-05 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-05 22:15:00 +0700
-- source showtime_id: 42daca7d-29ad-45ee-bcfc-125d73f03cf8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e743f336ba4f5f9f95e92fc51e07439e') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-05 15:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:30:00' AND ends_at > '2026-10-05 15:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e743f336ba4f5f9f95e92fc51e07439e'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-05 15:15:00', '2026-10-05 17:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-06 14:00:00 +0700
-- source showtime_id: 8ec21599-896e-47a3-b7d2-9186b2803f4c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8bc0df9fb6775d689186090f079af605') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-06 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:15:00' AND ends_at > '2026-10-06 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8bc0df9fb6775d689186090f079af605'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-06 07:00:00', '2026-10-06 09:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-06 16:45:00 +0700
-- source showtime_id: 5c79de69-e93a-489d-baa9-74789883d7a2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('757a06e2fe4b5ef49af36a9ef95af3bc') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-06 09:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:00:00' AND ends_at > '2026-10-06 09:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('757a06e2fe4b5ef49af36a9ef95af3bc'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-06 09:45:00', '2026-10-06 12:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-06 19:30:00 +0700
-- source showtime_id: 3069f093-0865-4911-ae6b-c6080cae6e0b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7061433e0dff5bd0866474dd373dcb59') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-06 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:45:00' AND ends_at > '2026-10-06 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7061433e0dff5bd0866474dd373dcb59'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-06 12:30:00', '2026-10-06 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-06 22:15:00 +0700
-- source showtime_id: 7a56a63c-cc30-4bdd-a075-44d359d3d6b3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9f0706dc2b025044b013efc2d5fb8ee7') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-06 15:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:30:00' AND ends_at > '2026-10-06 15:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9f0706dc2b025044b013efc2d5fb8ee7'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-06 15:15:00', '2026-10-06 17:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 7; local 2026-10-07 20:00:00 +0700
-- source showtime_id: 7af6dc59-ab47-4b1b-abc5-6266b2c6be78
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('43e4566443da5d3d841a9e1991412d4f') OR (room_id = @cinestar_seed_room_36 AND (starts_at = '2026-10-07 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:15:00' AND ends_at > '2026-10-07 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('43e4566443da5d3d841a9e1991412d4f'), @cinestar_seed_movie_15, @cinestar_seed_room_36, '2026-10-07 13:00:00', '2026-10-07 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_36 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-05 11:40:00 +0700
-- source showtime_id: eecc85c9-2b7c-4f48-89ce-8ec3da8bcb2a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('00ff7c7099de54a1999e97c74b0a07bd') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-05 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:35:00' AND ends_at > '2026-10-05 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('00ff7c7099de54a1999e97c74b0a07bd'), @cinestar_seed_movie_07, @cinestar_seed_room_37, '2026-10-05 04:40:00', '2026-10-05 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-05 14:00:00 +0700
-- source showtime_id: 799e2577-49f6-4fa7-8817-c834f33e8852
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ffbe07f66e2153d2ba5d4863672f4b74') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-05 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:01:00' AND ends_at > '2026-10-05 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ffbe07f66e2153d2ba5d4863672f4b74'), @cinestar_seed_movie_14, @cinestar_seed_room_37, '2026-10-05 07:00:00', '2026-10-05 09:01:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-05 16:30:00 +0700
-- source showtime_id: c5ab8897-c8cb-4fde-8a59-645f55c9cc27
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('694a5c4fb5e15d658c47767e4ad44c3b') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-05 09:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:25:00' AND ends_at > '2026-10-05 09:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('694a5c4fb5e15d658c47767e4ad44c3b'), @cinestar_seed_movie_07, @cinestar_seed_room_37, '2026-10-05 09:30:00', '2026-10-05 11:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-05 18:50:00 +0700
-- source showtime_id: a8b778bd-e1b7-4718-8b73-db8a76d448d6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c00e3a7bac8550fd810c26ce80b2e4d9') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-05 11:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:05:00' AND ends_at > '2026-10-05 11:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c00e3a7bac8550fd810c26ce80b2e4d9'), @cinestar_seed_movie_15, @cinestar_seed_room_37, '2026-10-05 11:50:00', '2026-10-05 14:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-05 21:30:00 +0700
-- source showtime_id: 415d498f-0025-48fc-8353-5af80969ae33
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('10ec3a84dc625788999bd71498bab06d') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-05 14:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:45:00' AND ends_at > '2026-10-05 14:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('10ec3a84dc625788999bd71498bab06d'), @cinestar_seed_movie_15, @cinestar_seed_room_37, '2026-10-05 14:30:00', '2026-10-05 16:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 09:00:00 +0700
-- source showtime_id: ad571a44-512f-4836-8c97-6e55535361f1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('eac24fad462358e4838c705b733b3289') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:15:00' AND ends_at > '2026-10-06 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('eac24fad462358e4838c705b733b3289'), @cinestar_seed_movie_15, @cinestar_seed_room_37, '2026-10-06 02:00:00', '2026-10-06 04:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 11:40:00 +0700
-- source showtime_id: 5c5cd944-1a1a-4534-b710-953d17f7e8bb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6b91e25d854e543cb9f40e3b7700a81a') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 04:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:35:00' AND ends_at > '2026-10-06 04:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6b91e25d854e543cb9f40e3b7700a81a'), @cinestar_seed_movie_07, @cinestar_seed_room_37, '2026-10-06 04:40:00', '2026-10-06 06:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 14:00:00 +0700
-- source showtime_id: d5642ba5-d620-4d2e-92db-d2f0282ed231
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cb6fa770cefd569aaa83ccc928635b8c') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 07:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:01:00' AND ends_at > '2026-10-06 07:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cb6fa770cefd569aaa83ccc928635b8c'), @cinestar_seed_movie_14, @cinestar_seed_room_37, '2026-10-06 07:00:00', '2026-10-06 09:01:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! LT (T13); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 16:30:00 +0700
-- source showtime_id: 55d54573-a137-4178-8d4d-b6ca42769f4e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9be1137007fa5050a8e35c1c4dd098b9') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 09:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:25:00' AND ends_at > '2026-10-06 09:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9be1137007fa5050a8e35c1c4dd098b9'), @cinestar_seed_movie_07, @cinestar_seed_room_37, '2026-10-06 09:30:00', '2026-10-06 11:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_07 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 18:50:00 +0700
-- source showtime_id: 68bfa798-777a-42e1-b646-47027171ff23
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8eeaeb49972b5e8e8e8fec3eff163a99') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 11:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:05:00' AND ends_at > '2026-10-06 11:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8eeaeb49972b5e8e8e8fec3eff163a99'), @cinestar_seed_movie_15, @cinestar_seed_room_37, '2026-10-06 11:50:00', '2026-10-06 14:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 01; local 2026-10-06 21:30:00 +0700
-- source showtime_id: e3373685-46d9-40ee-bcaa-430dca8bb6ec
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('aaa6dfeeb66c557da83d69aa13145614') OR (room_id = @cinestar_seed_room_37 AND (starts_at = '2026-10-06 14:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:45:00' AND ends_at > '2026-10-06 14:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('aaa6dfeeb66c557da83d69aa13145614'), @cinestar_seed_movie_15, @cinestar_seed_room_37, '2026-10-06 14:30:00', '2026-10-06 16:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_37 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 11:15:00 +0700
-- source showtime_id: 8b77b426-8be5-48a3-9d09-a97997b74bf9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('05321b6fa3f45d21b04f4bff94b8815d') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 04:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:35:00' AND ends_at > '2026-10-05 04:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('05321b6fa3f45d21b04f4bff94b8815d'), @cinestar_seed_movie_08, @cinestar_seed_room_38, '2026-10-05 04:15:00', '2026-10-05 05:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 13:00:00 +0700
-- source showtime_id: 79511c38-296e-4c8c-bc6a-2c8cb61d4864
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('209b854eaf575d7e98a4d34830e3a4d6') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:47:00' AND ends_at > '2026-10-05 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('209b854eaf575d7e98a4d34830e3a4d6'), @cinestar_seed_movie_13, @cinestar_seed_room_38, '2026-10-05 06:00:00', '2026-10-05 07:47:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 15:10:00 +0700
-- source showtime_id: d52e7fee-4fa1-413a-bf7e-83ab1013de2b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('731d77d4a7e751f2b7d417e7c79aa240') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:46:00' AND ends_at > '2026-10-05 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('731d77d4a7e751f2b7d417e7c79aa240'), @cinestar_seed_movie_06, @cinestar_seed_room_38, '2026-10-05 08:10:00', '2026-10-05 09:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 17:10:00 +0700
-- source showtime_id: d8d3d271-4f73-459d-886b-46f9a4aba986
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7cd1f78fcff055569cb83840869a886b') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 10:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:46:00' AND ends_at > '2026-10-05 10:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7cd1f78fcff055569cb83840869a886b'), @cinestar_seed_movie_06, @cinestar_seed_room_38, '2026-10-05 10:10:00', '2026-10-05 11:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 19:10:00 +0700
-- source showtime_id: 69d53e6e-28e8-4fab-9a36-05959cc04084
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a502ff2ae2275cc0b550372d2ed7a60f') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:30:00' AND ends_at > '2026-10-05 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a502ff2ae2275cc0b550372d2ed7a60f'), @cinestar_seed_movie_08, @cinestar_seed_room_38, '2026-10-05 12:10:00', '2026-10-05 13:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 20:55:00 +0700
-- source showtime_id: d3a59f64-51a7-4ac7-8266-4e4699d3f057
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2c59bdf62a3159ce9eafaadf58c84bac') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 13:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:11:00' AND ends_at > '2026-10-05 13:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2c59bdf62a3159ce9eafaadf58c84bac'), @cinestar_seed_movie_02, @cinestar_seed_room_38, '2026-10-05 13:55:00', '2026-10-05 15:11:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-05 22:40:00 +0700
-- source showtime_id: a04a1e09-2a92-40fd-a9f2-0a8a508d2a8a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('81de46ab802958c193cbeaf914cbd363') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:43:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('81de46ab802958c193cbeaf914cbd363'), @cinestar_seed_movie_20, @cinestar_seed_room_38, '2026-10-05 15:40:00', '2026-10-05 18:43:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 08:55:00 +0700
-- source showtime_id: b84a993c-07e3-4677-93d5-ee7dc1af15f0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c49d5715bd3e5a3dbb15cf3ac1194f0c') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 01:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:50:00' AND ends_at > '2026-10-06 01:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c49d5715bd3e5a3dbb15cf3ac1194f0c'), @cinestar_seed_movie_04, @cinestar_seed_room_38, '2026-10-06 01:55:00', '2026-10-06 03:50:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 11:15:00 +0700
-- source showtime_id: e4c39400-7963-4dac-a3fb-9702b3d070b6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6eb193a017265cbf9a80eef47b64f736') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 04:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:35:00' AND ends_at > '2026-10-06 04:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6eb193a017265cbf9a80eef47b64f736'), @cinestar_seed_movie_08, @cinestar_seed_room_38, '2026-10-06 04:15:00', '2026-10-06 05:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 13:00:00 +0700
-- source showtime_id: b7523c84-b3aa-4407-96cb-b5ba30452489
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fc958077a6875eed97994c9671b188b2') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:47:00' AND ends_at > '2026-10-06 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fc958077a6875eed97994c9671b188b2'), @cinestar_seed_movie_13, @cinestar_seed_room_38, '2026-10-06 06:00:00', '2026-10-06 07:47:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 15:10:00 +0700
-- source showtime_id: d1b6e1ef-5067-4050-bf84-4c18088321b1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c0bdcd244c6a5b4cad828fa05884124f') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:46:00' AND ends_at > '2026-10-06 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c0bdcd244c6a5b4cad828fa05884124f'), @cinestar_seed_movie_06, @cinestar_seed_room_38, '2026-10-06 08:10:00', '2026-10-06 09:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 17:10:00 +0700
-- source showtime_id: dd5f0d2f-5cde-4a48-9961-bbb97220879e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0141abfa7a4158ba8928e878022d94eb') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 10:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:46:00' AND ends_at > '2026-10-06 10:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0141abfa7a4158ba8928e878022d94eb'), @cinestar_seed_movie_06, @cinestar_seed_room_38, '2026-10-06 10:10:00', '2026-10-06 11:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 19:10:00 +0700
-- source showtime_id: 8781d0ce-efe5-4c30-b588-676f9b73c8c2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2527161529fa5b18b556d93d7a9c5d4b') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:30:00' AND ends_at > '2026-10-06 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2527161529fa5b18b556d93d7a9c5d4b'), @cinestar_seed_movie_08, @cinestar_seed_room_38, '2026-10-06 12:10:00', '2026-10-06 13:30:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 20:55:00 +0700
-- source showtime_id: 8cfdb5fa-d2fc-449f-bd0c-dc6654f3cb23
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e7aebe99f5fc55f2ae307d564b85c1d9') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 13:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:10:00' AND ends_at > '2026-10-06 13:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e7aebe99f5fc55f2ae307d564b85c1d9'), @cinestar_seed_movie_15, @cinestar_seed_room_38, '2026-10-06 13:55:00', '2026-10-06 16:10:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Đà Lạt (Lâm Đồng); room 02; local 2026-10-06 23:35:00 +0700
-- source showtime_id: ea2792c2-9e43-4dda-a5af-7b5d40cff142
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5a5e92b03f28518f99020a2dc3ab2e77') OR (room_id = @cinestar_seed_room_38 AND (starts_at = '2026-10-06 16:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:16:00' AND ends_at > '2026-10-06 16:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5a5e92b03f28518f99020a2dc3ab2e77'), @cinestar_seed_movie_29, @cinestar_seed_room_38, '2026-10-06 16:35:00', '2026-10-06 18:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_38 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 10:35:00 +0700
-- source showtime_id: 472e4497-4caa-4de7-94c4-ae468e489cd5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ccb06e91e456565493452915a787d70a') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 03:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:37:00' AND ends_at > '2026-10-05 03:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ccb06e91e456565493452915a787d70a'), @cinestar_seed_movie_05, @cinestar_seed_room_39, '2026-10-05 03:35:00', '2026-10-05 05:37:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 13:05:00 +0700
-- source showtime_id: 64c54c61-2264-4f92-aeaf-7281ff05ca83
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('71b30f23257b5f2eba93378ee719b9dd') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 06:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:21:00' AND ends_at > '2026-10-05 06:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('71b30f23257b5f2eba93378ee719b9dd'), @cinestar_seed_movie_02, @cinestar_seed_room_39, '2026-10-05 06:05:00', '2026-10-05 07:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 14:50:00 +0700
-- source showtime_id: fad774c5-e204-46a3-8e85-8046eeb33c57
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ddff050c2418561ab1bce44130cd827c') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:05:00' AND ends_at > '2026-10-05 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ddff050c2418561ab1bce44130cd827c'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-05 07:50:00', '2026-10-05 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 17:30:00 +0700
-- source showtime_id: e9ddf5a6-22f7-4722-9650-9079a9656a17
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('80e4e3b30adf53778604f8359278b66b') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 10:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:45:00' AND ends_at > '2026-10-05 10:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('80e4e3b30adf53778604f8359278b66b'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-05 10:30:00', '2026-10-05 12:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 20:10:00 +0700
-- source showtime_id: a7795ee5-72f7-40dd-9850-f0c413c73a61
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('46ac5e9b5889556582da09e254aae4ba') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 13:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:25:00' AND ends_at > '2026-10-05 13:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('46ac5e9b5889556582da09e254aae4ba'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-05 13:10:00', '2026-10-05 15:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-05 22:50:00 +0700
-- source showtime_id: 3b34d2bc-9c81-4ee6-9db2-348654f87fcf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b1c485f988fa528dbb57ccc8d6cd6758') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-05 15:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:05:00' AND ends_at > '2026-10-05 15:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b1c485f988fa528dbb57ccc8d6cd6758'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-05 15:50:00', '2026-10-05 18:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 08:00:00 +0700
-- source showtime_id: f57aa6cd-7285-4aaa-b66e-e5cf5207c51d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('baa1ff17fc415b34ae40bcbbe92e69d5') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:15:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('baa1ff17fc415b34ae40bcbbe92e69d5'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-06 01:00:00', '2026-10-06 03:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 10:35:00 +0700
-- source showtime_id: 3c0eea93-c264-4945-ad4d-97cea516f30c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0962f06e30a45eab8f30ceafcfda930d') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 03:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:37:00' AND ends_at > '2026-10-06 03:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0962f06e30a45eab8f30ceafcfda930d'), @cinestar_seed_movie_05, @cinestar_seed_room_39, '2026-10-06 03:35:00', '2026-10-06 05:37:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 13:05:00 +0700
-- source showtime_id: cced5240-8eaa-4e59-8890-aa868f98b6c4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ca5f211ea33059db99a1f95926da6ff8') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 06:05:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:21:00' AND ends_at > '2026-10-06 06:05:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ca5f211ea33059db99a1f95926da6ff8'), @cinestar_seed_movie_02, @cinestar_seed_room_39, '2026-10-06 06:05:00', '2026-10-06 07:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:05:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 14:50:00 +0700
-- source showtime_id: a008c5eb-47bc-4077-8c0e-346690b709a1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0bef90b96cb95aa68cd842c4ee91cea8') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 07:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:05:00' AND ends_at > '2026-10-06 07:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0bef90b96cb95aa68cd842c4ee91cea8'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-06 07:50:00', '2026-10-06 10:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 17:30:00 +0700
-- source showtime_id: abf796ec-e83d-4c6c-b361-c0850aad38d4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8587e906779c5af097d8bbaf3b0d9228') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 10:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:45:00' AND ends_at > '2026-10-06 10:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8587e906779c5af097d8bbaf3b0d9228'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-06 10:30:00', '2026-10-06 12:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 20:10:00 +0700
-- source showtime_id: b0468280-dd15-4163-b937-b3c83e29d194
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1cb85aeae48e5920a1deff33e5bac205') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 13:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:25:00' AND ends_at > '2026-10-06 13:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1cb85aeae48e5920a1deff33e5bac205'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-06 13:10:00', '2026-10-06 15:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-06 22:50:00 +0700
-- source showtime_id: 181296d3-8f52-4b27-a7c2-5e8dc58bf8be
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('58701f09c9605ad7b19e6511ef83b7e6') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-06 15:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:05:00' AND ends_at > '2026-10-06 15:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('58701f09c9605ad7b19e6511ef83b7e6'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-06 15:50:00', '2026-10-06 18:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-07 21:30:00 +0700
-- source showtime_id: 770ebdde-3b4e-4345-a19f-32ec30988352
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('dd9dc609f72353b5a4c1383a0c33f7bf') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-07 14:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:45:00' AND ends_at > '2026-10-07 14:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('dd9dc609f72353b5a4c1383a0c33f7bf'), @cinestar_seed_movie_15, @cinestar_seed_room_39, '2026-10-07 14:30:00', '2026-10-07 16:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Đà Lạt (Lâm Đồng); room 03; local 2026-10-12 19:00:00 +0700
-- source showtime_id: 08454c72-16b5-40fd-ac43-64bc9eb6af18
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f2b4e14d6456535487306c5f97ff500b') OR (room_id = @cinestar_seed_room_39 AND (starts_at = '2026-10-12 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-12 13:38:00' AND ends_at > '2026-10-12 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f2b4e14d6456535487306c5f97ff500b'), @cinestar_seed_movie_12, @cinestar_seed_room_39, '2026-10-12 12:00:00', '2026-10-12 13:38:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_39 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-12 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 10:55:00 +0700
-- source showtime_id: f15ba827-a447-413d-9daf-28a75e3e6f3b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c16729ab9e7c5ce5bac74dd61e3e14a9') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 03:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:10:00' AND ends_at > '2026-10-05 03:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c16729ab9e7c5ce5bac74dd61e3e14a9'), @cinestar_seed_movie_15, @cinestar_seed_room_40, '2026-10-05 03:55:00', '2026-10-05 06:10:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 03:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 13:35:00 +0700
-- source showtime_id: 4740070b-9760-4bd8-80eb-c32d2c33caf1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fb9c568281a85bb3ad04ead3e24cba3e') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 06:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:16:00' AND ends_at > '2026-10-05 06:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fb9c568281a85bb3ad04ead3e24cba3e'), @cinestar_seed_movie_29, @cinestar_seed_room_40, '2026-10-05 06:35:00', '2026-10-05 08:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 15:40:00 +0700
-- source showtime_id: 4daa51a7-5a8e-4b4c-b3a4-f4585947259b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b8ee18e019665f129c096ab2c2b2f9e6') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:43:00' AND ends_at > '2026-10-05 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b8ee18e019665f129c096ab2c2b2f9e6'), @cinestar_seed_movie_20, @cinestar_seed_room_40, '2026-10-05 08:40:00', '2026-10-05 11:43:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 19:10:00 +0700
-- source showtime_id: edf02b26-bea0-4155-9e03-d433e34f8f43
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('59eed67b941a52178be56d33de0ab24e') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:11:00' AND ends_at > '2026-10-05 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('59eed67b941a52178be56d33de0ab24e'), @cinestar_seed_movie_14, @cinestar_seed_room_40, '2026-10-05 12:10:00', '2026-10-05 14:11:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 21:40:00 +0700
-- source showtime_id: a85919cb-1744-4297-8e1d-4a3a2a54d1ac
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b4a0926af3d257379baa8e62d0a33116') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:21:00' AND ends_at > '2026-10-05 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b4a0926af3d257379baa8e62d0a33116'), @cinestar_seed_movie_29, @cinestar_seed_room_40, '2026-10-05 14:40:00', '2026-10-05 16:21:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-05 23:45:00 +0700
-- source showtime_id: 0706bec1-a3d3-4b1c-a785-c9c39c03b4de
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f95994d5f238508db113a5ca56f7c9b7') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-05 16:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:26:00' AND ends_at > '2026-10-05 16:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f95994d5f238508db113a5ca56f7c9b7'), @cinestar_seed_movie_29, @cinestar_seed_room_40, '2026-10-05 16:45:00', '2026-10-05 18:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 08:25:00 +0700
-- source showtime_id: cb4cc8e7-dd97-4257-8215-12ec81ca3a6b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2caaf0174aba5444b3634edfd9917885') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 01:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:26:00' AND ends_at > '2026-10-06 01:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2caaf0174aba5444b3634edfd9917885'), @cinestar_seed_movie_14, @cinestar_seed_room_40, '2026-10-06 01:25:00', '2026-10-06 03:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 10:55:00 +0700
-- source showtime_id: d03616ab-a0c4-43ac-b4f9-5400b7167327
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('47957ac3372e5bd0b5a6b93c439c25c0') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 03:55:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:10:00' AND ends_at > '2026-10-06 03:55:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('47957ac3372e5bd0b5a6b93c439c25c0'), @cinestar_seed_movie_15, @cinestar_seed_room_40, '2026-10-06 03:55:00', '2026-10-06 06:10:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:55:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 13:35:00 +0700
-- source showtime_id: 19a7bfb1-02f7-4c37-a7a0-6f170e2b53b4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('72aa35c1c09c58c987d233f35c865be8') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 06:35:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:16:00' AND ends_at > '2026-10-06 06:35:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('72aa35c1c09c58c987d233f35c865be8'), @cinestar_seed_movie_29, @cinestar_seed_room_40, '2026-10-06 06:35:00', '2026-10-06 08:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:35:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 15:40:00 +0700
-- source showtime_id: fbd4c788-330b-4d46-96ed-ca452ee17187
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8340ed5d366a5202bcb976e841f4e31c') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:43:00' AND ends_at > '2026-10-06 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8340ed5d366a5202bcb976e841f4e31c'), @cinestar_seed_movie_20, @cinestar_seed_room_40, '2026-10-06 08:40:00', '2026-10-06 11:43:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 19:10:00 +0700
-- source showtime_id: 06c059c5-ab47-4a8c-ab10-8a5e92c42783
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('510d45c1162d5bb499150fedd51a54bc') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 12:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:11:00' AND ends_at > '2026-10-06 12:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('510d45c1162d5bb499150fedd51a54bc'), @cinestar_seed_movie_14, @cinestar_seed_room_40, '2026-10-06 12:10:00', '2026-10-06 14:11:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- AVENGERS: HỒI KẾT - PHIÊN BẢN ĐẶC BIỆT (CHIẾU LẠI) (T13); Cinestar Đà Lạt (Lâm Đồng); room 05; local 2026-10-06 21:40:00 +0700
-- source showtime_id: 72fe6b6f-904a-4d17-9b3e-cfe8c49ba01f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('9f6781c19a6d5395899b9d5577868d79') OR (room_id = @cinestar_seed_room_40 AND (starts_at = '2026-10-06 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:43:00' AND ends_at > '2026-10-06 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('9f6781c19a6d5395899b9d5577868d79'), @cinestar_seed_movie_20, @cinestar_seed_room_40, '2026-10-06 14:40:00', '2026-10-06 17:43:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_40 AND @cinestar_seed_movie_20 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-05 12:50:00 +0700
-- source showtime_id: 1083601e-eae7-450c-a59a-4e97177958e8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ed1b416f4e505b05b6ffd0028fe5e3ab') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-05 05:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:05:00' AND ends_at > '2026-10-05 05:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ed1b416f4e505b05b6ffd0028fe5e3ab'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-05 05:50:00', '2026-10-05 08:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-05 15:30:00 +0700
-- source showtime_id: 10b5572b-7682-4284-b6f9-3f464c5e9856
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4e87415460365febb57fd6c662c4ed75') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-05 08:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:45:00' AND ends_at > '2026-10-05 08:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4e87415460365febb57fd6c662c4ed75'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-05 08:30:00', '2026-10-05 10:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-05 18:10:00 +0700
-- source showtime_id: 0996343e-ce8e-4947-b2cb-9aa4c4ea3d6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('841a1059745b5413989ef2dd8450cc17') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-05 11:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:25:00' AND ends_at > '2026-10-05 11:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('841a1059745b5413989ef2dd8450cc17'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-05 11:10:00', '2026-10-05 13:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-05 20:50:00 +0700
-- source showtime_id: 230caf66-f12d-4704-9966-cc00312f6f7c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c91abe5f117d5fe0a5070ed6694fab3f') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-05 13:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:05:00' AND ends_at > '2026-10-05 13:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c91abe5f117d5fe0a5070ed6694fab3f'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-05 13:50:00', '2026-10-05 16:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-05 23:30:00 +0700
-- source showtime_id: 25850181-2f9d-4280-a2b0-82d6d2ea91cc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ddad183d8ab65012bf98e058c92b0dec') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-05 16:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:45:00' AND ends_at > '2026-10-05 16:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ddad183d8ab65012bf98e058c92b0dec'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-05 16:30:00', '2026-10-05 18:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 08:00:00 +0700
-- source showtime_id: 61d2cfeb-6d70-4c20-a84c-a83bbe9dbd13
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0898e731fc9f5e16aa2bf31f4ca3881b') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 01:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:47:00' AND ends_at > '2026-10-06 01:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0898e731fc9f5e16aa2bf31f4ca3881b'), @cinestar_seed_movie_13, @cinestar_seed_room_41, '2026-10-06 01:00:00', '2026-10-06 02:47:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 10:10:00 +0700
-- source showtime_id: 05a58639-289c-460c-907f-5df5af5f4d4f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5772b6dfb9b358afb7f27be27ab67545') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 03:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:25:00' AND ends_at > '2026-10-06 03:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5772b6dfb9b358afb7f27be27ab67545'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 03:10:00', '2026-10-06 05:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 12:50:00 +0700
-- source showtime_id: 63ca714f-a19e-47dc-98ad-9196afaba748
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('46f45e92e417536dab9e440c277a5c29') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 05:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:05:00' AND ends_at > '2026-10-06 05:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('46f45e92e417536dab9e440c277a5c29'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 05:50:00', '2026-10-06 08:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 15:30:00 +0700
-- source showtime_id: ec670066-105d-4bdb-95b4-6ea6a6014fe5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('05041a191018592b8a1cb5b279c4165d') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 08:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:45:00' AND ends_at > '2026-10-06 08:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('05041a191018592b8a1cb5b279c4165d'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 08:30:00', '2026-10-06 10:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 18:10:00 +0700
-- source showtime_id: cb108d90-d3ce-4590-821a-9167ad144474
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('813c288c9d5750f8b181fdf94a0b3ede') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 11:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:25:00' AND ends_at > '2026-10-06 11:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('813c288c9d5750f8b181fdf94a0b3ede'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 11:10:00', '2026-10-06 13:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 20:50:00 +0700
-- source showtime_id: 172f67d5-3de7-4628-a2dc-0f54fb001af8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f1ba12ab071351d1b103d67ee2ed0a68') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 13:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:05:00' AND ends_at > '2026-10-06 13:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f1ba12ab071351d1b103d67ee2ed0a68'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 13:50:00', '2026-10-06 16:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-06 23:30:00 +0700
-- source showtime_id: 40d43fc4-328e-4ea6-bea2-dd77d9e0397d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d6947775c4dc5005ba87f6ae35c8907d') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-06 16:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:45:00' AND ends_at > '2026-10-06 16:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d6947775c4dc5005ba87f6ae35c8907d'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-06 16:30:00', '2026-10-06 18:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-07 12:30:00 +0700
-- source showtime_id: d7ba5fc0-c778-4302-93cc-1fe6e8f2659a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ca8d536493c35d0ea983a80aac32ecb8') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-07 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:45:00' AND ends_at > '2026-10-07 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ca8d536493c35d0ea983a80aac32ecb8'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-07 05:30:00', '2026-10-07 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-07 15:10:00 +0700
-- source showtime_id: 057f9bf3-9f1f-4cea-8ed3-2c96655c769a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('25483f83bcc95156aa07c9139eb78688') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-07 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:25:00' AND ends_at > '2026-10-07 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('25483f83bcc95156aa07c9139eb78688'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-07 08:10:00', '2026-10-07 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-07 17:50:00 +0700
-- source showtime_id: 1d235509-51bc-4620-89ac-527c49e6f722
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('32f4882ac14d5a66bd4ce6f83de30484') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-07 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 13:05:00' AND ends_at > '2026-10-07 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('32f4882ac14d5a66bd4ce6f83de30484'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-07 10:50:00', '2026-10-07 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-07 20:30:00 +0700
-- source showtime_id: 8e568ebf-ae7e-431a-9fa3-3378571f673f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('93a5ce173d9759d2bbaa508ec0610d00') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-07 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:45:00' AND ends_at > '2026-10-07 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('93a5ce173d9759d2bbaa508ec0610d00'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-07 13:30:00', '2026-10-07 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Đà Lạt (Lâm Đồng); room 06; local 2026-10-07 23:30:00 +0700
-- source showtime_id: 4075e7c1-2a57-4a9d-a90c-ea73337e7630
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('02f9e3d43c30538d8da07ef6678476a5') OR (room_id = @cinestar_seed_room_41 AND (starts_at = '2026-10-07 16:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 18:45:00' AND ends_at > '2026-10-07 16:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('02f9e3d43c30538d8da07ef6678476a5'), @cinestar_seed_movie_15, @cinestar_seed_room_41, '2026-10-07 16:30:00', '2026-10-07 18:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_41 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-05 11:00:00 +0700
-- source showtime_id: 0f3ac99e-278a-461b-98db-bae761f8eadb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('466b96488f3651f1a6480d29d181d850') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-05 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:15:00' AND ends_at > '2026-10-05 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('466b96488f3651f1a6480d29d181d850'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-05 04:00:00', '2026-10-05 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-05 13:40:00 +0700
-- source showtime_id: 51829612-869d-4c6f-952f-b4c435a249f7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2a14b912618f564e86c5da5e35c9a93f') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-05 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:55:00' AND ends_at > '2026-10-05 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2a14b912618f564e86c5da5e35c9a93f'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-05 06:40:00', '2026-10-05 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-05 16:20:00 +0700
-- source showtime_id: fedceaac-ce7f-4eb3-b33b-ddec8fc9c114
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('76536243b1725af0afb0d77e75c4288d') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-05 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:35:00' AND ends_at > '2026-10-05 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('76536243b1725af0afb0d77e75c4288d'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-05 09:20:00', '2026-10-05 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-05 19:00:00 +0700
-- source showtime_id: 0a148c7c-a034-4fbf-b8b6-98bb64897efe
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('75b86375f7db58f8b624048599539b11') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-05 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:15:00' AND ends_at > '2026-10-05 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('75b86375f7db58f8b624048599539b11'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-05 12:00:00', '2026-10-05 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-05 21:40:00 +0700
-- source showtime_id: 1ef4462e-c3d0-44d2-9f97-6c15a3e3be2e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6c0ca49b51735dbb91ac5add97bdf471') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-05 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:55:00' AND ends_at > '2026-10-05 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6c0ca49b51735dbb91ac5add97bdf471'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-05 14:40:00', '2026-10-05 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 08:25:00 +0700
-- source showtime_id: a1145ba9-99bc-41dd-b4aa-15af43507c6a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e497dcd4dee15db3b12bee9ddc7dfe49') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 01:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:40:00' AND ends_at > '2026-10-06 01:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e497dcd4dee15db3b12bee9ddc7dfe49'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 01:25:00', '2026-10-06 03:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 11:00:00 +0700
-- source showtime_id: 9a538a4a-765c-494f-95d3-a22ceb8f8409
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('803cc21e40e653fd91cd4b5dcdce4fed') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:15:00' AND ends_at > '2026-10-06 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('803cc21e40e653fd91cd4b5dcdce4fed'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 04:00:00', '2026-10-06 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 13:40:00 +0700
-- source showtime_id: b15b2736-c81c-475b-8788-2810e19b4cf7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3ed467d3d5ab53ddb936fa25f22d699c') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:55:00' AND ends_at > '2026-10-06 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3ed467d3d5ab53ddb936fa25f22d699c'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 06:40:00', '2026-10-06 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 16:20:00 +0700
-- source showtime_id: ea145087-f392-493e-ae9d-73e7f8730715
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bdd55e65e2e05e02986e25216648a8d1') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:35:00' AND ends_at > '2026-10-06 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bdd55e65e2e05e02986e25216648a8d1'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 09:20:00', '2026-10-06 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 19:00:00 +0700
-- source showtime_id: 684b0a23-e7dc-4340-a512-3cdd233bd19f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8fca66f5a5b25f89a27cd3e3b87a650c') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:15:00' AND ends_at > '2026-10-06 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8fca66f5a5b25f89a27cd3e3b87a650c'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 12:00:00', '2026-10-06 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-06 21:40:00 +0700
-- source showtime_id: e7cd863c-4bf0-42ec-8d62-6492409057cb
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ca149eb0447e5cfdba551fe34c4b0cf9') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-06 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:55:00' AND ends_at > '2026-10-06 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ca149eb0447e5cfdba551fe34c4b0cf9'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-06 14:40:00', '2026-10-06 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 08:25:00 +0700
-- source showtime_id: 9ec6f71c-3a28-4b38-949a-5d1a7888f8da
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d3fcde0147295b5f8673f4f992ec0bf8') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 01:25:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:40:00' AND ends_at > '2026-10-07 01:25:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d3fcde0147295b5f8673f4f992ec0bf8'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 01:25:00', '2026-10-07 03:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:25:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 11:00:00 +0700
-- source showtime_id: dbd7fda2-84a1-4be1-9289-31dab84f641d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3580c95a25bd54828f84fa1f9f3cfb07') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:15:00' AND ends_at > '2026-10-07 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3580c95a25bd54828f84fa1f9f3cfb07'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 04:00:00', '2026-10-07 06:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 13:40:00 +0700
-- source showtime_id: 146996cc-e5b2-4c3e-96ba-7906a94dce43
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('59f91391e95c522cb478ed4429648fa2') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 06:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:55:00' AND ends_at > '2026-10-07 06:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('59f91391e95c522cb478ed4429648fa2'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 06:40:00', '2026-10-07 08:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 16:20:00 +0700
-- source showtime_id: 220f4802-4095-4138-bf1d-bc81b1ab0499
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('500471271fe95d8797fa9e3da47733bf') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 09:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:35:00' AND ends_at > '2026-10-07 09:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('500471271fe95d8797fa9e3da47733bf'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 09:20:00', '2026-10-07 11:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 19:00:00 +0700
-- source showtime_id: 9cdf1e8d-e38a-4bc3-9e18-03e5f8a01625
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('07fdb6701158514886250d9d6c58207e') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 12:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:15:00' AND ends_at > '2026-10-07 12:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('07fdb6701158514886250d9d6c58207e'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 12:00:00', '2026-10-07 14:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 01; local 2026-10-07 21:40:00 +0700
-- source showtime_id: b881d2ab-8b7b-43e7-8e08-d38b6ef99e1d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4cf7fb87ff415ebeb9ad011054283894') OR (room_id = @cinestar_seed_room_42 AND (starts_at = '2026-10-07 14:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:55:00' AND ends_at > '2026-10-07 14:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4cf7fb87ff415ebeb9ad011054283894'), @cinestar_seed_movie_15, @cinestar_seed_room_42, '2026-10-07 14:40:00', '2026-10-07 16:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_42 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-05 12:00:00 +0700
-- source showtime_id: bdf59cd2-46c4-4230-8704-76a38e71afad
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a3b21cfed8485d868dcf701c492b8d88') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-05 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:15:00' AND ends_at > '2026-10-05 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a3b21cfed8485d868dcf701c492b8d88'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-05 05:00:00', '2026-10-05 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-05 14:40:00 +0700
-- source showtime_id: 219e52a0-ac68-4256-bce1-8457c03cbf83
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3ab47406c09a5443a14410e388f2da44') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-05 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:55:00' AND ends_at > '2026-10-05 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3ab47406c09a5443a14410e388f2da44'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-05 07:40:00', '2026-10-05 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-05 17:20:00 +0700
-- source showtime_id: 12565bcf-85b4-4e5f-aa14-9614ea9f17c3
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4e9f1e17a8445bc3b9883ab76f6db615') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-05 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:35:00' AND ends_at > '2026-10-05 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4e9f1e17a8445bc3b9883ab76f6db615'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-05 10:20:00', '2026-10-05 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-05 20:00:00 +0700
-- source showtime_id: de8d137f-8a07-4b74-87cb-b1be4d3fd401
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2dfde2ad02c95c8185c98794f22105e4') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-05 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:15:00' AND ends_at > '2026-10-05 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2dfde2ad02c95c8185c98794f22105e4'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-05 13:00:00', '2026-10-05 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-05 22:40:00 +0700
-- source showtime_id: c375d778-1b1e-4a70-b380-c6624e9d1775
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cad8e723b2ca59ddb7c3c28f909c65bb') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-05 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:55:00' AND ends_at > '2026-10-05 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cad8e723b2ca59ddb7c3c28f909c65bb'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-05 15:40:00', '2026-10-05 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 09:20:00 +0700
-- source showtime_id: f431785b-f095-4bc3-b92c-53c2a194c9bf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('4abb115ed28d5dacbc2584834bb81eab') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 02:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:35:00' AND ends_at > '2026-10-06 02:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('4abb115ed28d5dacbc2584834bb81eab'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 02:20:00', '2026-10-06 04:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 12:00:00 +0700
-- source showtime_id: 01d78d51-a1c9-4914-a792-8b435f695263
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c6ee66853786576fa27ab4360c99c468') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:15:00' AND ends_at > '2026-10-06 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c6ee66853786576fa27ab4360c99c468'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 05:00:00', '2026-10-06 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 14:40:00 +0700
-- source showtime_id: fdf3a34c-c9c0-4d9e-ab57-2f414065593e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c86daaf538385802ba1546461f3a2de4') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:55:00' AND ends_at > '2026-10-06 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c86daaf538385802ba1546461f3a2de4'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 07:40:00', '2026-10-06 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 17:20:00 +0700
-- source showtime_id: 0c953831-a82d-4ba8-9602-5705e5cb06d1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5166b43f7f9859b69d9935e2a58ab742') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:35:00' AND ends_at > '2026-10-06 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5166b43f7f9859b69d9935e2a58ab742'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 10:20:00', '2026-10-06 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 20:00:00 +0700
-- source showtime_id: 7248d5db-77a0-4da8-bd80-f3f804b813d8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('92df92e022335e47af7a038bcbc7c399') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:15:00' AND ends_at > '2026-10-06 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('92df92e022335e47af7a038bcbc7c399'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 13:00:00', '2026-10-06 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-06 22:40:00 +0700
-- source showtime_id: 61b17fa1-3746-4205-adcb-ccbff771d612
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('175f93e397845ae4af6c1fad343d7ee1') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-06 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:55:00' AND ends_at > '2026-10-06 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('175f93e397845ae4af6c1fad343d7ee1'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-06 15:40:00', '2026-10-06 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-07 12:00:00 +0700
-- source showtime_id: 179b3923-b5af-4134-b3f8-9d814a6e197d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('de204047c10f53578c50555e90ae33a6') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-07 05:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:15:00' AND ends_at > '2026-10-07 05:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('de204047c10f53578c50555e90ae33a6'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-07 05:00:00', '2026-10-07 07:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-07 14:40:00 +0700
-- source showtime_id: 0fa8f3ac-dc23-490b-a86c-71a43ed91a1d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d90df038bb305582ba04b9f0fbe1e366') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-07 07:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:55:00' AND ends_at > '2026-10-07 07:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d90df038bb305582ba04b9f0fbe1e366'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-07 07:40:00', '2026-10-07 09:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-07 17:20:00 +0700
-- source showtime_id: 21bb52b4-464b-4965-9e1c-077a66c08935
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2bd523b1c1e151fb95512ba003801cf8') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-07 10:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:35:00' AND ends_at > '2026-10-07 10:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2bd523b1c1e151fb95512ba003801cf8'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-07 10:20:00', '2026-10-07 12:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-07 20:00:00 +0700
-- source showtime_id: 276358d7-3b28-487b-9465-846bc18f7f79
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('16130e233fdb5583ac6ac974f543ff24') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-07 13:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:15:00' AND ends_at > '2026-10-07 13:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('16130e233fdb5583ac6ac974f543ff24'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-07 13:00:00', '2026-10-07 15:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 02; local 2026-10-07 22:40:00 +0700
-- source showtime_id: 74483f30-f90a-4173-8da1-d71a4bfdb1d6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('29c2b90e922f5e94a8ad1be09a29454a') OR (room_id = @cinestar_seed_room_43 AND (starts_at = '2026-10-07 15:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 17:55:00' AND ends_at > '2026-10-07 15:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('29c2b90e922f5e94a8ad1be09a29454a'), @cinestar_seed_movie_15, @cinestar_seed_room_43, '2026-10-07 15:40:00', '2026-10-07 17:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_43 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-05 11:30:00 +0700
-- source showtime_id: eedf3296-6ea5-4d64-bcb1-63ae20fc848c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('195850ffccb95f4ca5d0defde578a12b') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-05 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 06:45:00' AND ends_at > '2026-10-05 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('195850ffccb95f4ca5d0defde578a12b'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-05 04:30:00', '2026-10-05 06:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-05 14:10:00 +0700
-- source showtime_id: 2b22d814-fd43-44ee-a5db-00af96eb8a27
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('96d06223b19a592cb634378bba0444d6') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-05 07:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:25:00' AND ends_at > '2026-10-05 07:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('96d06223b19a592cb634378bba0444d6'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-05 07:10:00', '2026-10-05 09:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-05 16:50:00 +0700
-- source showtime_id: 487c6df2-38ec-4221-8154-a3f8610d961a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f7103a8c8a515d7dbf48aad47dadd58f') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-05 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:05:00' AND ends_at > '2026-10-05 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f7103a8c8a515d7dbf48aad47dadd58f'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-05 09:50:00', '2026-10-05 12:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-05 19:30:00 +0700
-- source showtime_id: 84486f03-6bce-4ce2-a021-abc76f1ad3db
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1cdc9fb91acb52d9957dbe25a8496379') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-05 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 14:45:00' AND ends_at > '2026-10-05 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1cdc9fb91acb52d9957dbe25a8496379'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-05 12:30:00', '2026-10-05 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-05 22:10:00 +0700
-- source showtime_id: da454351-8278-489c-a4e2-20813d9ea817
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c434d714820d51e8ae861964571486b6') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-05 15:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:25:00' AND ends_at > '2026-10-05 15:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c434d714820d51e8ae861964571486b6'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-05 15:10:00', '2026-10-05 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 08:50:00 +0700
-- source showtime_id: 8cd236ea-164f-4d19-b5af-b0220c5d3e81
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('866b55b81dba50a89dba8e46bb1808b9') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 01:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:05:00' AND ends_at > '2026-10-06 01:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('866b55b81dba50a89dba8e46bb1808b9'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 01:50:00', '2026-10-06 04:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 11:30:00 +0700
-- source showtime_id: 2908800b-84d7-40a2-a1ca-9fec8fe3d4e9
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1ce2f03060a251e2a2277825844bdc96') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 06:45:00' AND ends_at > '2026-10-06 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1ce2f03060a251e2a2277825844bdc96'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 04:30:00', '2026-10-06 06:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 14:10:00 +0700
-- source showtime_id: 0c419d2e-88b9-4d2c-ba13-9596781d036b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('2b7b7bd353665c49961aaaaf52e2931e') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 07:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:25:00' AND ends_at > '2026-10-06 07:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('2b7b7bd353665c49961aaaaf52e2931e'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 07:10:00', '2026-10-06 09:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 16:50:00 +0700
-- source showtime_id: 5974d4e2-9d1e-4392-aee7-3b1adefff021
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c07faa47e3c550608c0e48342158c85e') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:05:00' AND ends_at > '2026-10-06 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c07faa47e3c550608c0e48342158c85e'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 09:50:00', '2026-10-06 12:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 19:30:00 +0700
-- source showtime_id: c6ecf533-bd22-4c50-b72d-db4c80e8d74e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('232b858ca7375d4dbc5dedebf2587e24') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 14:45:00' AND ends_at > '2026-10-06 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('232b858ca7375d4dbc5dedebf2587e24'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 12:30:00', '2026-10-06 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-06 22:10:00 +0700
-- source showtime_id: dd40ba36-a2a0-4b9b-b0e0-1c63607ed4e7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('694d10b0cc225c71814f574998dc6110') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-06 15:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:25:00' AND ends_at > '2026-10-06 15:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('694d10b0cc225c71814f574998dc6110'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-06 15:10:00', '2026-10-06 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 08:50:00 +0700
-- source showtime_id: 46bef1ed-7107-446f-bda6-2fe703d55d03
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8539a23046d3594f9e9809076792c3cd') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 01:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 04:05:00' AND ends_at > '2026-10-07 01:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8539a23046d3594f9e9809076792c3cd'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 01:50:00', '2026-10-07 04:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 01:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 11:30:00 +0700
-- source showtime_id: 1e98340c-44b9-4466-af0b-b18164f8da85
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3170598af98657149e8fb97aaf3ebb7c') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 04:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:45:00' AND ends_at > '2026-10-07 04:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3170598af98657149e8fb97aaf3ebb7c'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 04:30:00', '2026-10-07 06:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 14:10:00 +0700
-- source showtime_id: 0eefee1f-bd18-4394-8f13-a1457dc8662a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c4f5eafdb31a5bb693f3be3b7d6ecc3b') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 07:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 09:25:00' AND ends_at > '2026-10-07 07:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c4f5eafdb31a5bb693f3be3b7d6ecc3b'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 07:10:00', '2026-10-07 09:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 16:50:00 +0700
-- source showtime_id: 9575e479-f29b-4b8e-8dd9-e77388f346b0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c7f71ae8889653319a907971e43974a3') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 09:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 12:05:00' AND ends_at > '2026-10-07 09:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c7f71ae8889653319a907971e43974a3'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 09:50:00', '2026-10-07 12:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 19:30:00 +0700
-- source showtime_id: 9975997f-df4f-4fa8-af02-11818ad7ad35
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e966c53c9ce4559eb4db65e668c41dd6') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 12:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 14:45:00' AND ends_at > '2026-10-07 12:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e966c53c9ce4559eb4db65e668c41dd6'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 12:30:00', '2026-10-07 14:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 12:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 03; local 2026-10-07 22:10:00 +0700
-- source showtime_id: d3048203-8be8-4a40-bdca-2c5cae1a117a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('863b7e2c2a7250e3960b244c03fdf587') OR (room_id = @cinestar_seed_room_44 AND (starts_at = '2026-10-07 15:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 17:25:00' AND ends_at > '2026-10-07 15:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('863b7e2c2a7250e3960b244c03fdf587'), @cinestar_seed_movie_15, @cinestar_seed_room_44, '2026-10-07 15:10:00', '2026-10-07 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_44 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 15:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-05 12:30:00 +0700
-- source showtime_id: bfb0b578-b08a-480a-8282-cb6aeb0129cd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6c875bf6026b5c8e9e0126333862da3e') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-05 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:45:00' AND ends_at > '2026-10-05 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6c875bf6026b5c8e9e0126333862da3e'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-05 05:30:00', '2026-10-05 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-05 15:10:00 +0700
-- source showtime_id: f52f2285-f3f8-4d2b-a043-23416f99f082
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('13f73df686ed5a7cbb98bed3f9356d97') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-05 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:25:00' AND ends_at > '2026-10-05 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('13f73df686ed5a7cbb98bed3f9356d97'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-05 08:10:00', '2026-10-05 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-05 17:50:00 +0700
-- source showtime_id: 4bbc111c-8f49-43a0-b0db-e77d5af29b6e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cd0541f381f25f8fb6e28510bffab235') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-05 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:05:00' AND ends_at > '2026-10-05 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cd0541f381f25f8fb6e28510bffab235'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-05 10:50:00', '2026-10-05 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-05 20:30:00 +0700
-- source showtime_id: 6e5e235c-302c-429a-8510-3edaab7c0cb4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5397951b21695f9cac2109b41376a8f8') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-05 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:45:00' AND ends_at > '2026-10-05 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5397951b21695f9cac2109b41376a8f8'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-05 13:30:00', '2026-10-05 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-05 23:10:00 +0700
-- source showtime_id: dd747501-f9aa-482b-bd80-5ad742660537
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b51a9df415645d57a1c835fb89fab0a4') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-05 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:25:00' AND ends_at > '2026-10-05 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b51a9df415645d57a1c835fb89fab0a4'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-05 16:10:00', '2026-10-05 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 09:50:00 +0700
-- source showtime_id: 5d89cd3e-6c91-4fd0-b6cb-0ad205e3be28
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e8849bffe3ab5167aa237ac35c6dc660') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:05:00' AND ends_at > '2026-10-06 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e8849bffe3ab5167aa237ac35c6dc660'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 02:50:00', '2026-10-06 05:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 12:30:00 +0700
-- source showtime_id: 0bae9ff1-1db9-4d4b-8900-07dfb194a890
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('55ffea042bfc587c853ae9efed6a207d') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:45:00' AND ends_at > '2026-10-06 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('55ffea042bfc587c853ae9efed6a207d'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 05:30:00', '2026-10-06 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 15:10:00 +0700
-- source showtime_id: 27b8e1a4-fb3d-4fa0-8c92-181cb7341927
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('16be1b050ffb56339fb468715eb42a8c') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:25:00' AND ends_at > '2026-10-06 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('16be1b050ffb56339fb468715eb42a8c'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 08:10:00', '2026-10-06 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 17:50:00 +0700
-- source showtime_id: 0135ed5c-8da0-467e-8b12-c20ee2f08060
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ef602c0933755c23905cac6fae388c93') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:05:00' AND ends_at > '2026-10-06 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ef602c0933755c23905cac6fae388c93'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 10:50:00', '2026-10-06 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 20:30:00 +0700
-- source showtime_id: 483b4099-a363-4965-8ef0-d979104bdd4d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('480c2920dd51598e8d36fba3211bee9a') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:45:00' AND ends_at > '2026-10-06 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('480c2920dd51598e8d36fba3211bee9a'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 13:30:00', '2026-10-06 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-06 23:10:00 +0700
-- source showtime_id: 7731c76f-decc-4c98-8864-8bed977187c5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('59da5b6e325e52e4b71e99412435bdf3') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-06 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:25:00' AND ends_at > '2026-10-06 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('59da5b6e325e52e4b71e99412435bdf3'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-06 16:10:00', '2026-10-06 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 09:50:00 +0700
-- source showtime_id: 55726a42-9997-4cb9-a353-bef501d05622
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d9d746c57e875b1f8242d93bfa9f05c9') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 02:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 05:05:00' AND ends_at > '2026-10-07 02:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d9d746c57e875b1f8242d93bfa9f05c9'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 02:50:00', '2026-10-07 05:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 12:30:00 +0700
-- source showtime_id: 7e2d4377-4a66-4aec-bc53-5dc329cbb376
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('69d3d61d181955e58f3d52af1a8bdf4d') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 05:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 07:45:00' AND ends_at > '2026-10-07 05:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('69d3d61d181955e58f3d52af1a8bdf4d'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 05:30:00', '2026-10-07 07:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 15:10:00 +0700
-- source showtime_id: e3b82765-6a0b-479b-a59e-59565ed04e16
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0f2356c8cf1657e0b43cff309ad1e8e1') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 08:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:25:00' AND ends_at > '2026-10-07 08:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0f2356c8cf1657e0b43cff309ad1e8e1'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 08:10:00', '2026-10-07 10:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 17:50:00 +0700
-- source showtime_id: bc3e1a98-8d94-45fb-b08c-c052c072a484
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('1fa123aed8325a319be76252bb04363c') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 10:50:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 13:05:00' AND ends_at > '2026-10-07 10:50:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('1fa123aed8325a319be76252bb04363c'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 10:50:00', '2026-10-07 13:05:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 10:50:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 20:30:00 +0700
-- source showtime_id: b8053011-2ff9-4c52-8f1e-bd13351d1fe2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('65c39354eaf45a6ab1eac1d775f1017d') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 13:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 15:45:00' AND ends_at > '2026-10-07 13:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('65c39354eaf45a6ab1eac1d775f1017d'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 13:30:00', '2026-10-07 15:45:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 13:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 06; local 2026-10-07 23:10:00 +0700
-- source showtime_id: b2c8e57f-83b5-4156-94ff-8d475b51092d
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('02a69322a7f050008e7656dd791660b4') OR (room_id = @cinestar_seed_room_45 AND (starts_at = '2026-10-07 16:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 18:25:00' AND ends_at > '2026-10-07 16:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('02a69322a7f050008e7656dd791660b4'), @cinestar_seed_movie_15, @cinestar_seed_room_45, '2026-10-07 16:10:00', '2026-10-07 18:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_45 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 11:15:00 +0700
-- source showtime_id: 1d11e43e-a673-460a-bd30-5d16a7fe656e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('47a8e3fc0580570d95849123db82dd95') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 04:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 05:31:00' AND ends_at > '2026-10-05 04:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('47a8e3fc0580570d95849123db82dd95'), @cinestar_seed_movie_02, @cinestar_seed_room_46, '2026-10-05 04:15:00', '2026-10-05 05:31:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 04:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 13:00:00 +0700
-- source showtime_id: bb5856e9-fcc1-44e7-9816-aa74e8c541bc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5678f596501650b3afd3550635b01ddf') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:15:00' AND ends_at > '2026-10-05 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5678f596501650b3afd3550635b01ddf'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-05 06:00:00', '2026-10-05 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 15:40:00 +0700
-- source showtime_id: 53432e06-ced4-444b-8909-cec5fd090afa
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('93b16fbd330456ffb6f75db010477fb2') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 10:55:00' AND ends_at > '2026-10-05 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('93b16fbd330456ffb6f75db010477fb2'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-05 08:40:00', '2026-10-05 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 18:20:00 +0700
-- source showtime_id: 8345b54d-9a09-4b10-9932-870b671b4423
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bed26b9e912e5ce9a5d68e2684801cf1') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:35:00' AND ends_at > '2026-10-05 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bed26b9e912e5ce9a5d68e2684801cf1'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-05 11:20:00', '2026-10-05 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 21:00:00 +0700
-- source showtime_id: d6c8865e-c992-47ce-8b03-27bebc20d3ed
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('8bd42528f3c959e9b477bdf6f3dc919e') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:15:00' AND ends_at > '2026-10-05 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('8bd42528f3c959e9b477bdf6f3dc919e'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-05 14:00:00', '2026-10-05 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-05 23:40:00 +0700
-- source showtime_id: 6ca1fb12-f87a-4cc4-aae0-219716ac4bfa
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6820e3d394b3576d8b597e61d6e4386c') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-05 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:27:00' AND ends_at > '2026-10-05 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6820e3d394b3576d8b597e61d6e4386c'), @cinestar_seed_movie_13, @cinestar_seed_room_46, '2026-10-05 16:40:00', '2026-10-05 18:27:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 08:30:00 +0700
-- source showtime_id: ea3e911b-7c5e-4ea2-bc78-ecf545ecf268
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d616bddf837351458b209b16130bb4a7') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 01:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 03:06:00' AND ends_at > '2026-10-06 01:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d616bddf837351458b209b16130bb4a7'), @cinestar_seed_movie_06, @cinestar_seed_room_46, '2026-10-06 01:30:00', '2026-10-06 03:06:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- KHÓA CHẶT CỬA NÀO SUZUME (P); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 10:30:00 +0700
-- source showtime_id: c031f730-f2fd-4d5f-9015-e380ee8ccf0e
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7067bd6b19b85becbcce7a3172640c25') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 03:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:32:00' AND ends_at > '2026-10-06 03:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7067bd6b19b85becbcce7a3172640c25'), @cinestar_seed_movie_05, @cinestar_seed_room_46, '2026-10-06 03:30:00', '2026-10-06 05:32:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_05 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 13:00:00 +0700
-- source showtime_id: ec680d98-8a7d-4ce0-a8f5-c8861c1ced0f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0e2f8ffe5d6355f09843130a0a03f229') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:15:00' AND ends_at > '2026-10-06 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0e2f8ffe5d6355f09843130a0a03f229'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-06 06:00:00', '2026-10-06 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 15:40:00 +0700
-- source showtime_id: 6cc81264-0d35-4523-8760-0778803a8826
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f8b486275f57532ea5c3e73bdd6cb327') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 10:55:00' AND ends_at > '2026-10-06 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f8b486275f57532ea5c3e73bdd6cb327'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-06 08:40:00', '2026-10-06 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 18:20:00 +0700
-- source showtime_id: c0d355ae-6e9d-4089-9776-2735a7e1af99
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5b05a3265b5d52449c275532ebe9ee76') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:35:00' AND ends_at > '2026-10-06 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5b05a3265b5d52449c275532ebe9ee76'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-06 11:20:00', '2026-10-06 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 21:00:00 +0700
-- source showtime_id: 2e6cca63-279a-4f9c-91c4-57da455cb412
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e233e8022ca95dc580bcddd47651d7d4') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:15:00' AND ends_at > '2026-10-06 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e233e8022ca95dc580bcddd47651d7d4'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-06 14:00:00', '2026-10-06 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-06 23:40:00 +0700
-- source showtime_id: 505e49a7-c2a9-4ceb-a7ee-1ce740a4ef0b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('30e1c848872f529995f2ed2e308b205b') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-06 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:27:00' AND ends_at > '2026-10-06 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('30e1c848872f529995f2ed2e308b205b'), @cinestar_seed_movie_13, @cinestar_seed_room_46, '2026-10-06 16:40:00', '2026-10-06 18:27:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- HÒN ĐẢO QUÊN LÃNG LT (K); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 09:00:00 +0700
-- source showtime_id: 085a5abc-bccc-4511-b0d3-32eee78eecf1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c0776fb7317f5de4803680a11ba89e33') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 02:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 03:49:00' AND ends_at > '2026-10-07 02:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c0776fb7317f5de4803680a11ba89e33'), @cinestar_seed_movie_01, @cinestar_seed_room_46, '2026-10-07 02:00:00', '2026-10-07 03:49:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_01 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 02:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 11:15:00 +0700
-- source showtime_id: 7854552f-0488-45d9-aabd-a5d5ee22ae42
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5c0b319e73cf52958eb5689503cea9af') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 04:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 05:31:00' AND ends_at > '2026-10-07 04:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5c0b319e73cf52958eb5689503cea9af'), @cinestar_seed_movie_02, @cinestar_seed_room_46, '2026-10-07 04:15:00', '2026-10-07 05:31:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 04:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 13:00:00 +0700
-- source showtime_id: 889b99f2-9f2c-4fe5-938a-af5e23458ecc
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('77532a9f777b54ee99016f678318a3be') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 06:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:15:00' AND ends_at > '2026-10-07 06:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('77532a9f777b54ee99016f678318a3be'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-07 06:00:00', '2026-10-07 08:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 06:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 15:40:00 +0700
-- source showtime_id: 44dcbb8b-0255-423b-81aa-c654a2b2e9fe
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('414ef1a13c405ab4ba583c6e5b06eae6') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 08:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 10:55:00' AND ends_at > '2026-10-07 08:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('414ef1a13c405ab4ba583c6e5b06eae6'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-07 08:40:00', '2026-10-07 10:55:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 08:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 18:20:00 +0700
-- source showtime_id: 18927688-76ca-4e2e-b497-5a7bdc39a9bf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0ff4333e00895104916570599d43caec') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 11:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 13:35:00' AND ends_at > '2026-10-07 11:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0ff4333e00895104916570599d43caec'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-07 11:20:00', '2026-10-07 13:35:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 11:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 21:00:00 +0700
-- source showtime_id: 6fb380ce-7785-4612-a56b-eccc7717c693
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ada1130da44a5bf49324ec11bb3db45e') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 14:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 16:15:00' AND ends_at > '2026-10-07 14:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ada1130da44a5bf49324ec11bb3db45e'), @cinestar_seed_movie_15, @cinestar_seed_room_46, '2026-10-07 14:00:00', '2026-10-07 16:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 14:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 07; local 2026-10-07 23:40:00 +0700
-- source showtime_id: 49fcca9b-7a02-40c8-b79a-11d291172cf5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('adfb4a58dd465cb9a1df82cc1e108c17') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-07 16:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 18:27:00' AND ends_at > '2026-10-07 16:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('adfb4a58dd465cb9a1df82cc1e108c17'), @cinestar_seed_movie_13, @cinestar_seed_room_46, '2026-10-07 16:40:00', '2026-10-07 18:27:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 16:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Huế (TP. Huế); room 07; local 2026-10-12 19:40:00 +0700
-- source showtime_id: 3562b845-0064-48f4-9fe2-410a0fdd56df
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d9d97c7dad5a5e7a96961ff91e12bfa1') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-12 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-12 14:18:00' AND ends_at > '2026-10-12 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d9d97c7dad5a5e7a96961ff91e12bfa1'), @cinestar_seed_movie_12, @cinestar_seed_room_46, '2026-10-12 12:40:00', '2026-10-12 14:18:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-12 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ALWAYS LALISA; Cinestar Huế (TP. Huế); room 07; local 2026-10-14 19:40:00 +0700
-- source showtime_id: 405745bc-2056-4eda-8915-d3c5b90dba3f
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b1ed7e1e245a5758a28d39ae95dedd47') OR (room_id = @cinestar_seed_room_46 AND (starts_at = '2026-10-14 12:40:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-14 14:18:00' AND ends_at > '2026-10-14 12:40:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b1ed7e1e245a5758a28d39ae95dedd47'), @cinestar_seed_movie_12, @cinestar_seed_room_46, '2026-10-14 12:40:00', '2026-10-14 14:18:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_46 AND @cinestar_seed_movie_12 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-14 12:40:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-05 12:45:00 +0700
-- source showtime_id: 78b4d89e-a30b-4e36-b777-0d3e266a7a42
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('af7b1b61496d5a23895330097939b4c5') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-05 05:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:26:00' AND ends_at > '2026-10-05 05:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('af7b1b61496d5a23895330097939b4c5'), @cinestar_seed_movie_29, @cinestar_seed_room_47, '2026-10-05 05:45:00', '2026-10-05 07:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-05 14:45:00 +0700
-- source showtime_id: 1e21b66a-dc84-4f71-b339-a36a648fc080
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d7d7c11deb0a56568a52b2bca199228c') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-05 07:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 09:40:00' AND ends_at > '2026-10-05 07:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d7d7c11deb0a56568a52b2bca199228c'), @cinestar_seed_movie_04, @cinestar_seed_room_47, '2026-10-05 07:45:00', '2026-10-05 09:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 08; local 2026-10-05 17:00:00 +0700
-- source showtime_id: aa86b6f6-9fb8-461c-909f-5c07714e6782
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('c48d5a88d70352a6a36966e5b2fd2d5b') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-05 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 12:15:00' AND ends_at > '2026-10-05 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('c48d5a88d70352a6a36966e5b2fd2d5b'), @cinestar_seed_movie_15, @cinestar_seed_room_47, '2026-10-05 10:00:00', '2026-10-05 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 08; local 2026-10-05 19:45:00 +0700
-- source showtime_id: 879eb079-7211-456c-929c-5d58109667d1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e686626a44935e0ca43b185ad9f9c89c') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-05 12:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 15:00:00' AND ends_at > '2026-10-05 12:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e686626a44935e0ca43b185ad9f9c89c'), @cinestar_seed_movie_15, @cinestar_seed_room_47, '2026-10-05 12:45:00', '2026-10-05 15:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 12:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-05 22:30:00 +0700
-- source showtime_id: eb8f8632-7485-45d5-899f-e692db36e516
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6055d4154e8a51deaa66b5f3d4861618') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-05 15:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 17:25:00' AND ends_at > '2026-10-05 15:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6055d4154e8a51deaa66b5f3d4861618'), @cinestar_seed_movie_04, @cinestar_seed_room_47, '2026-10-05 15:30:00', '2026-10-05 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 15:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- PHÁO HOA LÚC BÌNH MINH (P); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 11:00:00 +0700
-- source showtime_id: 1531e0a3-f58b-42bf-ac0d-223e89eac6cf
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('a7641f18033d5159821df3abdd32f9bf') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 04:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 05:16:00' AND ends_at > '2026-10-06 04:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('a7641f18033d5159821df3abdd32f9bf'), @cinestar_seed_movie_02, @cinestar_seed_room_47, '2026-10-06 04:00:00', '2026-10-06 05:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_02 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 04:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 12:45:00 +0700
-- source showtime_id: c52dcb7c-198b-4a78-bce3-5b6b68f03b0b
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('fcfa7527bd555f99a0bdc321ad8115fb') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 05:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:26:00' AND ends_at > '2026-10-06 05:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('fcfa7527bd555f99a0bdc321ad8115fb'), @cinestar_seed_movie_29, @cinestar_seed_room_47, '2026-10-06 05:45:00', '2026-10-06 07:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 14:45:00 +0700
-- source showtime_id: 49149cd9-e141-4e96-9495-f0d1db2d8a16
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b825906768fd5e219957470535dfb3b0') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 07:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 09:40:00' AND ends_at > '2026-10-06 07:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b825906768fd5e219957470535dfb3b0'), @cinestar_seed_movie_04, @cinestar_seed_room_47, '2026-10-06 07:45:00', '2026-10-06 09:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 17:00:00 +0700
-- source showtime_id: 129975b4-746b-4842-bb7c-585b5d909d07
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('f61a2f928f1b520eb99b876320183c38') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 10:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 12:15:00' AND ends_at > '2026-10-06 10:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('f61a2f928f1b520eb99b876320183c38'), @cinestar_seed_movie_15, @cinestar_seed_room_47, '2026-10-06 10:00:00', '2026-10-06 12:15:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 10:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRẠI BUÔN NGƯỜI (T18); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 19:45:00 +0700
-- source showtime_id: 074b9468-32f9-45bc-a11d-1133d58a2e41
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('196eb79e99b254589a74cae3972b86df') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 12:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 15:00:00' AND ends_at > '2026-10-06 12:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('196eb79e99b254589a74cae3972b86df'), @cinestar_seed_movie_15, @cinestar_seed_room_47, '2026-10-06 12:45:00', '2026-10-06 15:00:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_15 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 12:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- QUYẾT CUA ANH NÀY! (PĐ) (T13); Cinestar Huế (TP. Huế); room 08; local 2026-10-06 22:30:00 +0700
-- source showtime_id: d16e796b-16d8-4aa1-b1e3-04269b3c46ca
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('56685b28973a59f28d44ba39af1e3229') OR (room_id = @cinestar_seed_room_47 AND (starts_at = '2026-10-06 15:30:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 17:25:00' AND ends_at > '2026-10-06 15:30:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('56685b28973a59f28d44ba39af1e3229'), @cinestar_seed_movie_04, @cinestar_seed_room_47, '2026-10-06 15:30:00', '2026-10-06 17:25:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_47 AND @cinestar_seed_movie_04 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 15:30:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 12:15:00 +0700
-- source showtime_id: ad59d060-b7ab-4041-9b6e-fa54358451b4
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5a001c3ff86c5ee58d6276729baedb47') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 05:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 07:02:00' AND ends_at > '2026-10-05 05:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5a001c3ff86c5ee58d6276729baedb47'), @cinestar_seed_movie_13, @cinestar_seed_room_48, '2026-10-05 05:15:00', '2026-10-05 07:02:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 05:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 14:20:00 +0700
-- source showtime_id: 12ea5e58-c275-4551-8f76-fc5e7b6fd9a8
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('51c3f8736d335578b860e65f796facd5') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 08:56:00' AND ends_at > '2026-10-05 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('51c3f8736d335578b860e65f796facd5'), @cinestar_seed_movie_06, @cinestar_seed_room_48, '2026-10-05 07:20:00', '2026-10-05 08:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 16:15:00 +0700
-- source showtime_id: 098b26e5-c017-4956-8e81-24c432e26b8c
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('cde65bf82574549e9d6a5f8ed047b0a3') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 09:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 11:16:00' AND ends_at > '2026-10-05 09:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('cde65bf82574549e9d6a5f8ed047b0a3'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-05 09:15:00', '2026-10-05 11:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 09:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 18:45:00 +0700
-- source showtime_id: 3828a19e-ed36-4aa0-aa6c-c7916e1319a2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('7c5b41e1c67a55d0b2467fe5fae0cc42') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 11:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 13:46:00' AND ends_at > '2026-10-05 11:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('7c5b41e1c67a55d0b2467fe5fae0cc42'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-05 11:45:00', '2026-10-05 13:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 11:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 21:15:00 +0700
-- source showtime_id: 64f3281b-cc85-474e-8a1c-3e81865efae2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('52e224f9162d568e9cc33c51734ead7a') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 14:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 16:16:00' AND ends_at > '2026-10-05 14:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('52e224f9162d568e9cc33c51734ead7a'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-05 14:15:00', '2026-10-05 16:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 14:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Huế (TP. Huế); room 09; local 2026-10-05 23:45:00 +0700
-- source showtime_id: b10376cd-c067-43ad-b0f4-4bf4cd3d78a7
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('b79c4eb070185fc4a9bb03e758f1003c') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-05 16:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-05 18:26:00' AND ends_at > '2026-10-05 16:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('b79c4eb070185fc4a9bb03e758f1003c'), @cinestar_seed_movie_29, @cinestar_seed_room_48, '2026-10-05 16:45:00', '2026-10-05 18:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-05 16:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- SCOTTY: GIẢI CỨU HOÀNG THƯỢNG LT (P); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 08:20:00 +0700
-- source showtime_id: ec903fe2-0dbc-45ee-8a30-49efdd2a3eb6
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('3f8016f90712591481ba8d631bf7acd9') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 01:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 02:40:00' AND ends_at > '2026-10-06 01:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('3f8016f90712591481ba8d631bf7acd9'), @cinestar_seed_movie_08, @cinestar_seed_room_48, '2026-10-06 01:20:00', '2026-10-06 02:40:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_08 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 01:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- HÒN ĐẢO QUÊN LÃNG LT (K); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 10:00:00 +0700
-- source showtime_id: b6430665-c42e-4860-a17a-861e43f30ca5
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('34ab35171b205cf39174d6bb88837bf7') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 03:00:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 04:49:00' AND ends_at > '2026-10-06 03:00:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('34ab35171b205cf39174d6bb88837bf7'), @cinestar_seed_movie_01, @cinestar_seed_room_48, '2026-10-06 03:00:00', '2026-10-06 04:49:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_01 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 03:00:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 12:15:00 +0700
-- source showtime_id: 9e4e8b07-d349-49a0-b746-96a291d2dcd0
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e26a602a8a215d9cbb9eeec4f16c2db5') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 05:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 07:02:00' AND ends_at > '2026-10-06 05:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e26a602a8a215d9cbb9eeec4f16c2db5'), @cinestar_seed_movie_13, @cinestar_seed_room_48, '2026-10-06 05:15:00', '2026-10-06 07:02:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 05:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 14:20:00 +0700
-- source showtime_id: f5f2daea-631a-49ba-a951-7ec37b306580
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('ea9f1201e53e5337b0967e551d5ea44a') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 08:56:00' AND ends_at > '2026-10-06 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('ea9f1201e53e5337b0967e551d5ea44a'), @cinestar_seed_movie_06, @cinestar_seed_room_48, '2026-10-06 07:20:00', '2026-10-06 08:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 16:15:00 +0700
-- source showtime_id: bf1954a6-bc06-4c60-b3a6-9d62feeeb12a
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('5b1b96c914275500a4d91d44eca1674c') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 09:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 11:16:00' AND ends_at > '2026-10-06 09:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('5b1b96c914275500a4d91d44eca1674c'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-06 09:15:00', '2026-10-06 11:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 09:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 18:45:00 +0700
-- source showtime_id: a69e895d-281a-4416-a3ee-7efec2dd6d55
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('e815a91fcb2450a2824d6b5b2f7b39b2') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 11:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 13:46:00' AND ends_at > '2026-10-06 11:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('e815a91fcb2450a2824d6b5b2f7b39b2'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-06 11:45:00', '2026-10-06 13:46:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 11:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 21:15:00 +0700
-- source showtime_id: 952be486-a6ac-40b9-908e-d9f8768e5c56
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('0ec46802977b5abdb4d20988605a10e5') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 14:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 16:16:00' AND ends_at > '2026-10-06 14:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('0ec46802977b5abdb4d20988605a10e5'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-06 14:15:00', '2026-10-06 16:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 14:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- TRÁI TIM QUÁI THÚ (T13); Cinestar Huế (TP. Huế); room 09; local 2026-10-06 23:45:00 +0700
-- source showtime_id: 8ffaf9ed-2845-4e79-83c2-63dffa3eddd1
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('d06babad76c352ba81edbfa842a9bdf9') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-06 16:45:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-06 18:26:00' AND ends_at > '2026-10-06 16:45:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('d06babad76c352ba81edbfa842a9bdf9'), @cinestar_seed_movie_29, @cinestar_seed_room_48, '2026-10-06 16:45:00', '2026-10-06 18:26:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_29 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-06 16:45:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- ÚT LAN 2 (T18); Cinestar Huế (TP. Huế); room 09; local 2026-10-07 12:10:00 +0700
-- source showtime_id: 72483775-6c91-4ffe-9cca-595a105edafd
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('bc661f5e88685967b72bb4d4e6b00d57') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-07 05:10:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 06:57:00' AND ends_at > '2026-10-07 05:10:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('bc661f5e88685967b72bb4d4e6b00d57'), @cinestar_seed_movie_13, @cinestar_seed_room_48, '2026-10-07 05:10:00', '2026-10-07 06:57:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_13 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 05:10:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- THẦN SƯ CHUNG QUỲ: LINH GIỚI ĐẠI CHIẾN (T13); Cinestar Huế (TP. Huế); room 09; local 2026-10-07 14:20:00 +0700
-- source showtime_id: ad428065-5c79-47ca-8728-a7a9f89f6964
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('09246c571563522d8bfa8a2ef0b42cb8') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-07 07:20:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 08:56:00' AND ends_at > '2026-10-07 07:20:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('09246c571563522d8bfa8a2ef0b42cb8'), @cinestar_seed_movie_06, @cinestar_seed_room_48, '2026-10-07 07:20:00', '2026-10-07 08:56:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_06 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 07:20:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

-- LÊN HƯƠNG (T16); Cinestar Huế (TP. Huế); room 09; local 2026-10-07 16:15:00 +0700
-- source showtime_id: d4713314-60a3-4f3c-a739-37aabeb19ff2
SET @cinestar_seed_exists = (SELECT COUNT(*) FROM showtimes WHERE id = UNHEX('6e48527ca8e953b7a3eab45f472e10da') OR (room_id = @cinestar_seed_room_48 AND (starts_at = '2026-10-07 09:15:00' OR (status <> 'CANCELLED' AND starts_at < '2026-10-07 11:16:00' AND ends_at > '2026-10-07 09:15:00'))));
INSERT INTO showtimes (id, movie_id, room_id, starts_at, ends_at, status, version, created_at, updated_at)
SELECT UNHEX('6e48527ca8e953b7a3eab45f472e10da'), @cinestar_seed_movie_14, @cinestar_seed_room_48, '2026-10-07 09:15:00', '2026-10-07 11:16:00', 'SCHEDULED', 0, @cinestar_seed_now, @cinestar_seed_now
FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
WHERE r.id = @cinestar_seed_room_48 AND @cinestar_seed_movie_14 IS NOT NULL AND r.active = TRUE AND c.active = TRUE
  AND '2026-10-07 09:15:00' > UTC_TIMESTAMP(6)
  AND @cinestar_seed_exists = 0;

COMMIT;

-- 6. COUNTS (tong ban ghi hien co, khong phai so ban ghi vua INSERT).
USE cinema_movie_db;
SELECT 'movies' AS table_name, COUNT(*) AS total_rows FROM movies
UNION ALL SELECT 'genres', COUNT(*) FROM genres
UNION ALL SELECT 'movie_genres', COUNT(*) FROM movie_genres;
SELECT status, COUNT(*) AS total_rows FROM movies GROUP BY status;
USE cinema_inventory_db;
SELECT 'cinemas' AS table_name, COUNT(*) AS total_rows FROM cinemas
UNION ALL SELECT 'rooms', COUNT(*) FROM rooms
UNION ALL SELECT 'seats', COUNT(*) FROM seats
UNION ALL SELECT 'showtimes', COUNT(*) FROM showtimes
UNION ALL SELECT 'show_seats', COUNT(*) FROM show_seats;
SELECT status, COUNT(*) AS total_rows, MIN(starts_at) AS earliest_start_utc, MAX(starts_at) AS latest_start_utc FROM showtimes GROUP BY status;

SET SESSION time_zone = @cinestar_seed_previous_time_zone;
