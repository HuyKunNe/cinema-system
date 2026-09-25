SET NAMES utf8mb4;
USE cinema_movie_db;

START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);

-- ============================================================
-- GENRES
-- ============================================================

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
        UNHEX('20000000000040008000000000000001'),
        'Kinh dị',
        NULL,
        0,
        @now,
        @now
    ),
    (
        UNHEX('20000000000040008000000000000002'),
        'Khoa học viễn tưởng',
        NULL,
        0,
        @now,
        @now
    ),
    (
        UNHEX('20000000000040008000000000000003'),
        'Chính kịch',
        NULL,
        0,
        @now,
        @now
    ),
    (
        UNHEX('20000000000040008000000000000004'),
        'Gia đình',
        NULL,
        0,
        @now,
        @now
    )
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

-- ============================================================
-- MOVIES
--
-- Snapshot dữ liệu lịch chiếu CGV tháng 09/2026.
-- Không copy plot/description dài hoặc artwork từ CGV.
-- ============================================================

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
        UNHEX('10000000000040008000000000000001'),
        'Vùng Đất Quỷ Dữ',
        NULL,
        94,
        NULL,
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('10000000000040008000000000000002'),
        'Út Lan 2',
        NULL,
        107,
        '2026-09-25',
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('10000000000040008000000000000003'),
        'Lên Hương',
        NULL,
        121,
        NULL,
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('10000000000040008000000000000004'),
        'Trại Buôn Người',
        NULL,
        99,
        NULL,
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('10000000000040008000000000000005'),
        'Laputa: Lâu Đài Trên Không',
        NULL,
        129,
        NULL,
        NULL,
        NULL,
        'NOW_SHOWING',
        0,
        @now,
        @now
    )
ON DUPLICATE KEY UPDATE
    duration_minutes = VALUES(duration_minutes),
    release_date = VALUES(release_date),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

-- ============================================================
-- MOVIE GENRES
-- ============================================================

INSERT IGNORE INTO movie_genres (
    movie_id,
    genre_id
)
VALUES
    -- Vùng Đất Quỷ Dữ
    (
        UNHEX('10000000000040008000000000000001'),
        UNHEX('20000000000040008000000000000001')
    ),
    (
        UNHEX('10000000000040008000000000000001'),
        UNHEX('20000000000040008000000000000002')
    ),

    -- Út Lan 2
    (
        UNHEX('10000000000040008000000000000002'),
        UNHEX('20000000000040008000000000000001')
    ),

    -- Lên Hương
    (
        UNHEX('10000000000040008000000000000003'),
        UNHEX('20000000000040008000000000000003')
    ),
    (
        UNHEX('10000000000040008000000000000003'),
        UNHEX('20000000000040008000000000000004')
    );

COMMIT;
