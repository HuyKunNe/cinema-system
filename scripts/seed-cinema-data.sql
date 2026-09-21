-- Cinema System sample data
-- MySQL 8.x
--
-- Run this script after Flyway has created the tables for:
--   1. cinema_movie_db
--   2. cinema_inventory_db
--
-- The script is idempotent and can reuse records previously created by API.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ================================================================
-- Movie Service
-- ================================================================

USE cinema_movie_db;

START TRANSACTION;

SET @seed_now = UTC_TIMESTAMP(6);

-- Resolve an existing ID by unique name first. Use the fixed seed ID only
-- when the record does not exist yet.
SET @genre_action_id = COALESCE(
    (SELECT id FROM genres WHERE name = 'Hành động' LIMIT 1),
    UUID_TO_BIN('10000000-0000-0000-0000-000000000001')
);

SET @genre_science_fiction_id = COALESCE(
    (SELECT id FROM genres WHERE name = 'Khoa học viễn tưởng' LIMIT 1),
    UUID_TO_BIN('10000000-0000-0000-0000-000000000002')
);

SET @genre_adventure_id = COALESCE(
    (SELECT id FROM genres WHERE name = 'Phiêu lưu' LIMIT 1),
    UUID_TO_BIN('10000000-0000-0000-0000-000000000003')
);

SET @genre_animation_id = COALESCE(
    (SELECT id FROM genres WHERE name = 'Hoạt hình' LIMIT 1),
    UUID_TO_BIN('10000000-0000-0000-0000-000000000004')
);

SET @genre_horror_id = COALESCE(
    (SELECT id FROM genres WHERE name = 'Kinh dị' LIMIT 1),
    UUID_TO_BIN('10000000-0000-0000-0000-000000000005')
);

INSERT INTO genres (
    id,
    name,
    description,
    version,
    created_at,
    updated_at
)
VALUES
    (
        @genre_action_id,
        'Hành động',
        'Phim có nhiều cảnh hành động, chiến đấu và truy đuổi.',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @genre_science_fiction_id,
        'Khoa học viễn tưởng',
        'Phim khai thác công nghệ, không gian và thế giới tương lai.',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @genre_adventure_id,
        'Phiêu lưu',
        'Phim xoay quanh những hành trình khám phá và thử thách.',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @genre_animation_id,
        'Hoạt hình',
        'Phim hoạt hình dành cho nhiều nhóm khán giả.',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @genre_horror_id,
        'Kinh dị',
        'Phim có yếu tố hồi hộp, bí ẩn và kinh dị.',
        0,
        @seed_now,
        @seed_now
    )
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = VALUES(updated_at);

SET @movie_interstellar_id = COALESCE(
    (SELECT id FROM movies WHERE title = 'Interstellar' LIMIT 1),
    UUID_TO_BIN('20000000-0000-0000-0000-000000000001')
);

SET @movie_john_wick_4_id = COALESCE(
    (SELECT id FROM movies WHERE title = 'John Wick: Chapter 4' LIMIT 1),
    UUID_TO_BIN('20000000-0000-0000-0000-000000000002')
);

SET @movie_future_world_id = COALESCE(
    (SELECT id FROM movies WHERE title = 'Future World' LIMIT 1),
    UUID_TO_BIN('20000000-0000-0000-0000-000000000003')
);

INSERT INTO movies (
    id,
    title,
    description,
    duration_minutes,
    release_date,
    poster_url,
    trailer_url,
    status,
    version,
    created_at,
    updated_at
)
VALUES
    (
        @movie_interstellar_id,
        'Interstellar',
        'Một nhóm phi hành gia thực hiện chuyến du hành xuyên không gian để tìm kiếm nơi ở mới cho nhân loại.',
        169,
        '2014-11-07',
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @movie_john_wick_4_id,
        'John Wick: Chapter 4',
        'John Wick đối đầu với những thế lực mới trong thế giới sát thủ.',
        169,
        '2023-03-24',
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @seed_now,
        @seed_now
    ),
    (
        @movie_future_world_id,
        'Future World',
        'Một bộ phim khoa học viễn tưởng mẫu dùng để kiểm thử chức năng phim sắp chiếu.',
        125,
        '2026-10-15',
        NULL,
        NULL,
        'UPCOMING',
        0,
        @seed_now,
        @seed_now
    )
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    duration_minutes = VALUES(duration_minutes),
    release_date = VALUES(release_date),
    poster_url = VALUES(poster_url),
    trailer_url = VALUES(trailer_url),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT IGNORE INTO movie_genres (movie_id, genre_id)
VALUES
    (@movie_interstellar_id, @genre_science_fiction_id),
    (@movie_interstellar_id, @genre_adventure_id),
    (@movie_john_wick_4_id, @genre_action_id),
    (@movie_future_world_id, @genre_science_fiction_id),
    (@movie_future_world_id, @genre_adventure_id);

COMMIT;

-- ================================================================
-- Inventory Service
-- ================================================================

USE cinema_inventory_db;

START TRANSACTION;

SET @seed_now = UTC_TIMESTAMP(6);

-- cinemas.name is not unique in the schema, so the seed resolves the first
-- matching record before deciding to insert a new one.
SET @cinema_nguyen_hue_id = COALESCE(
    (
        SELECT id
        FROM cinemas
        WHERE name = 'Cinema Center Nguyễn Huệ'
        ORDER BY created_at
        LIMIT 1
    ),
    UUID_TO_BIN('30000000-0000-0000-0000-000000000001')
);

SET @cinema_landmark_id = COALESCE(
    (
        SELECT id
        FROM cinemas
        WHERE name = 'Cinema Center Landmark'
        ORDER BY created_at
        LIMIT 1
    ),
    UUID_TO_BIN('30000000-0000-0000-0000-000000000002')
);

INSERT INTO cinemas (
    id,
    name,
    address,
    city,
    active,
    version,
    created_at,
    updated_at
)
VALUES
    (
        @cinema_nguyen_hue_id,
        'Cinema Center Nguyễn Huệ',
        '22 Nguyễn Huệ, Phường Sài Gòn',
        'Hồ Chí Minh',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @cinema_landmark_id,
        'Cinema Center Landmark',
        '720A Điện Biên Phủ, Phường Thạnh Mỹ Tây',
        'Hồ Chí Minh',
        TRUE,
        0,
        @seed_now,
        @seed_now
    )
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    city = VALUES(city),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

SET @room_standard_id = COALESCE(
    (
        SELECT id
        FROM rooms
        WHERE cinema_id = @cinema_nguyen_hue_id
          AND name = 'Phòng 01'
        LIMIT 1
    ),
    UUID_TO_BIN('40000000-0000-0000-0000-000000000001')
);

SET @room_imax_id = COALESCE(
    (
        SELECT id
        FROM rooms
        WHERE cinema_id = @cinema_nguyen_hue_id
          AND name = 'Phòng IMAX 01'
        LIMIT 1
    ),
    UUID_TO_BIN('40000000-0000-0000-0000-000000000002')
);

INSERT INTO rooms (
    id,
    cinema_id,
    name,
    room_type,
    active,
    version,
    created_at,
    updated_at
)
VALUES
    (
        @room_standard_id,
        @cinema_nguyen_hue_id,
        'Phòng 01',
        'STANDARD',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @room_imax_id,
        @cinema_nguyen_hue_id,
        'Phòng IMAX 01',
        'IMAX',
        TRUE,
        0,
        @seed_now,
        @seed_now
    )
ON DUPLICATE KEY UPDATE
    room_type = VALUES(room_type),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

SET @seat_a1_id = COALESCE(
    (
        SELECT id FROM seats
        WHERE room_id = @room_standard_id AND seat_number = 'A1'
        LIMIT 1
    ),
    UUID_TO_BIN('50000000-0000-0000-0000-000000000001')
);

SET @seat_a2_id = COALESCE(
    (
        SELECT id FROM seats
        WHERE room_id = @room_standard_id AND seat_number = 'A2'
        LIMIT 1
    ),
    UUID_TO_BIN('50000000-0000-0000-0000-000000000002')
);

SET @seat_h7_id = COALESCE(
    (
        SELECT id FROM seats
        WHERE room_id = @room_standard_id AND seat_number = 'H7'
        LIMIT 1
    ),
    UUID_TO_BIN('50000000-0000-0000-0000-000000000003')
);

SET @seat_h8_id = COALESCE(
    (
        SELECT id FROM seats
        WHERE room_id = @room_standard_id AND seat_number = 'H8'
        LIMIT 1
    ),
    UUID_TO_BIN('50000000-0000-0000-0000-000000000004')
);

SET @seat_c1_id = COALESCE(
    (
        SELECT id FROM seats
        WHERE room_id = @room_standard_id AND seat_number = 'C1'
        LIMIT 1
    ),
    UUID_TO_BIN('50000000-0000-0000-0000-000000000005')
);

INSERT INTO seats (
    id,
    room_id,
    seat_number,
    row_label,
    seat_type,
    active,
    version,
    created_at,
    updated_at
)
VALUES
    (
        @seat_a1_id,
        @room_standard_id,
        'A1',
        'A',
        'STANDARD',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @seat_a2_id,
        @room_standard_id,
        'A2',
        'A',
        'STANDARD',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @seat_h7_id,
        @room_standard_id,
        'H7',
        'H',
        'VIP',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @seat_h8_id,
        @room_standard_id,
        'H8',
        'H',
        'VIP',
        TRUE,
        0,
        @seed_now,
        @seed_now
    ),
    (
        @seat_c1_id,
        @room_standard_id,
        'C1',
        'C',
        'COUPLE',
        TRUE,
        0,
        @seed_now,
        @seed_now
    )
ON DUPLICATE KEY UPDATE
    row_label = VALUES(row_label),
    seat_type = VALUES(seat_type),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

COMMIT;

-- ================================================================
-- Verification
-- ================================================================

SELECT
    BIN_TO_UUID(id) AS genre_id,
    name,
    description
FROM cinema_movie_db.genres
ORDER BY name;

SELECT
    BIN_TO_UUID(id) AS movie_id,
    title,
    status,
    release_date
FROM cinema_movie_db.movies
ORDER BY title;

SELECT
    BIN_TO_UUID(id) AS cinema_id,
    name,
    city,
    active
FROM cinema_inventory_db.cinemas
ORDER BY name;

SELECT
    BIN_TO_UUID(r.id) AS room_id,
    c.name AS cinema_name,
    r.name AS room_name,
    r.room_type,
    r.active
FROM cinema_inventory_db.rooms r
JOIN cinema_inventory_db.cinemas c ON c.id = r.cinema_id
ORDER BY c.name, r.name;

SELECT
    BIN_TO_UUID(s.id) AS seat_id,
    r.name AS room_name,
    s.seat_number,
    s.row_label,
    s.seat_type,
    s.active
FROM cinema_inventory_db.seats s
JOIN cinema_inventory_db.rooms r ON r.id = s.room_id
ORDER BY r.name, s.row_label, s.seat_number;
