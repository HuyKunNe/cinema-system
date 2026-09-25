SET NAMES utf8mb4;
USE cinema_inventory_db;

START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);

-- ============================================================
-- CINEMAS
-- ============================================================

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
        UNHEX('30000000000040008000000000000001'),
        'CGV Vincom Gò Vấp',
        'Tầng 5, TTTM Vincom Plaza Gò Vấp, 12 Phan Văn Trị, Phường 7, Quận Gò Vấp',
        'Hồ Chí Minh',
        TRUE,
        0,
        @now,
        @now
    ),
    (
        UNHEX('30000000000040008000000000000002'),
        'CGV Vincom Landmark 81',
        'B1, Vincom Center Landmark 81, 722 Điện Biên Phủ, Phường 22, Quận Bình Thạnh',
        'Hồ Chí Minh',
        TRUE,
        0,
        @now,
        @now
    )
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    city = VALUES(city),
    active = TRUE,
    updated_at = VALUES(updated_at);

-- ============================================================
-- ROOMS
--
-- Gò Vấp thực tế có 3 phòng.
-- Landmark 81 có nhiều phòng hơn nhưng seed chỉ tạo representative
-- rooms cần cho demo.
-- ============================================================

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
        UNHEX('40000000000040008000000000000011'),
        UNHEX('30000000000040008000000000000001'),
        'Phòng 1',
        'STANDARD',
        TRUE,
        0,
        @now,
        @now
    ),
    (
        UNHEX('40000000000040008000000000000012'),
        UNHEX('30000000000040008000000000000001'),
        'Phòng 2',
        'STANDARD',
        TRUE,
        0,
        @now,
        @now
    ),
    (
        UNHEX('40000000000040008000000000000013'),
        UNHEX('30000000000040008000000000000001'),
        'Phòng 3',
        'STANDARD',
        TRUE,
        0,
        @now,
        @now
    ),
    (
        UNHEX('40000000000040008000000000000021'),
        UNHEX('30000000000040008000000000000002'),
        'ScreenX',
        'SCREEN_X',
        TRUE,
        0,
        @now,
        @now
    ),
    (
        UNHEX('40000000000040008000000000000022'),
        UNHEX('30000000000040008000000000000002'),
        'Phòng 2D',
        'STANDARD',
        TRUE,
        0,
        @now,
        @now
    )
ON DUPLICATE KEY UPDATE
    room_type = VALUES(room_type),
    active = TRUE,
    updated_at = VALUES(updated_at);

-- ============================================================
-- DEMO SEAT LAYOUT
--
-- 6 rows × 8 seats = 48 seats / room.
-- A-D STANDARD
-- E-F VIP
--
-- Đây là layout demo, KHÔNG phải sơ đồ ghế CGV thật.
-- ============================================================

DROP TEMPORARY TABLE IF EXISTS seed_rows;
DROP TEMPORARY TABLE IF EXISTS seed_numbers;

CREATE TEMPORARY TABLE seed_rows (
    row_label VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50) NOT NULL
);

INSERT INTO seed_rows VALUES
    ('A', 'STANDARD'),
    ('B', 'STANDARD'),
    ('C', 'STANDARD'),
    ('D', 'STANDARD'),
    ('E', 'VIP'),
    ('F', 'VIP');

CREATE TEMPORARY TABLE seed_numbers (
    seat_no INT NOT NULL
);

INSERT INTO seed_numbers VALUES
    (1), (2), (3), (4),
    (5), (6), (7), (8);

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
SELECT
    UNHEX(
        MD5(
            CONCAT(
                'cgv-demo-seat:',
                HEX(r.id),
                ':',
                sr.row_label,
                ':',
                sn.seat_no
            )
        )
    ),
    r.id,
    CONCAT(sr.row_label, sn.seat_no),
    sr.row_label,
    sr.seat_type,
    TRUE,
    0,
    @now,
    @now
FROM rooms r
CROSS JOIN seed_rows sr
CROSS JOIN seed_numbers sn
WHERE r.id IN (
    UNHEX('40000000000040008000000000000011'),
    UNHEX('40000000000040008000000000000012'),
    UNHEX('40000000000040008000000000000013'),
    UNHEX('40000000000040008000000000000021'),
    UNHEX('40000000000040008000000000000022')
)
ON DUPLICATE KEY UPDATE
    seat_type = VALUES(seat_type),
    active = TRUE,
    updated_at = VALUES(updated_at);

-- ============================================================
-- SHOWTIMES
--
-- DATETIME values below are UTC.
--
-- Example:
--   26/09 09:10 Vietnam (+07)
--   => 26/09 02:10 UTC
--
-- Movie IDs match movie_seed.sql.
-- ============================================================

INSERT INTO showtimes (
    id,
    movie_id,
    room_id,
    starts_at,
    ends_at,
    status,
    version,
    created_at,
    updated_at
)
VALUES

    -- ========================================================
    -- LÊN HƯƠNG - CGV VINCOM GÒ VẤP
    -- 26/09: 09:10, 18:30, 20:40
    -- ========================================================

    (
        UNHEX('50000000000040008000000000000001'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000011'),
        '2026-09-26 02:10:00.000000',
        '2026-09-26 04:11:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('50000000000040008000000000000002'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000012'),
        '2026-09-26 11:30:00.000000',
        '2026-09-26 13:31:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('50000000000040008000000000000003'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000011'),
        '2026-09-26 13:40:00.000000',
        '2026-09-26 15:41:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),

    -- 27/09: 09:10, 17:10, 18:30, 20:40

    (
        UNHEX('50000000000040008000000000000004'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000011'),
        '2026-09-27 02:10:00.000000',
        '2026-09-27 04:11:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('50000000000040008000000000000005'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000011'),
        '2026-09-27 10:10:00.000000',
        '2026-09-27 12:11:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('50000000000040008000000000000006'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000012'),
        '2026-09-27 11:30:00.000000',
        '2026-09-27 13:31:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),
    (
        UNHEX('50000000000040008000000000000007'),
        UNHEX('10000000000040008000000000000003'),
        UNHEX('40000000000040008000000000000011'),
        '2026-09-27 13:40:00.000000',
        '2026-09-27 15:41:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),

    -- ========================================================
    -- VÙNG ĐẤT QUỶ DỮ - LANDMARK 81
    -- 26/09 ScreenX 23:40
    -- ========================================================

    (
        UNHEX('50000000000040008000000000000011'),
        UNHEX('10000000000040008000000000000001'),
        UNHEX('40000000000040008000000000000021'),
        '2026-09-26 16:40:00.000000',
        '2026-09-26 18:14:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),

    -- 27/09 08:50 2D

    (
        UNHEX('50000000000040008000000000000012'),
        UNHEX('10000000000040008000000000000001'),
        UNHEX('40000000000040008000000000000022'),
        '2026-09-27 01:50:00.000000',
        '2026-09-27 03:24:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    ),

    -- 27/09 ScreenX 23:40

    (
        UNHEX('50000000000040008000000000000013'),
        UNHEX('10000000000040008000000000000001'),
        UNHEX('40000000000040008000000000000021'),
        '2026-09-27 16:40:00.000000',
        '2026-09-27 18:14:00.000000',
        'OPEN_FOR_BOOKING',
        0,
        @now,
        @now
    )

ON DUPLICATE KEY UPDATE
    movie_id = VALUES(movie_id),
    room_id = VALUES(room_id),
    starts_at = VALUES(starts_at),
    ends_at = VALUES(ends_at),
    status = 'OPEN_FOR_BOOKING',
    updated_at = VALUES(updated_at);

-- ============================================================
-- SHOW SEATS
--
-- Demo pricing:
--
-- CGV Gò Vấp weekend:
-- STANDARD 120,000
-- VIP      125,000
--
-- Landmark 81:
-- STANDARD 130,000
-- VIP      135,000
--
-- Đây là demo approximation cho booking system.
-- ============================================================

INSERT INTO show_seats (
    id,
    showtime_id,
    seat_id,
    seat_number,
    seat_type,
    price,
    status,
    held_by_booking_id,
    hold_expires_at,
    version,
    created_at,
    updated_at
)
SELECT
    UNHEX(
        MD5(
            CONCAT(
                'cgv-demo-show-seat:',
                HEX(st.id),
                ':',
                HEX(s.id)
            )
        )
    ),
    st.id,
    s.id,
    s.seat_number,
    s.seat_type,

    CASE
        WHEN c.id =
            UNHEX('30000000000040008000000000000001')
            AND s.seat_type = 'VIP'
            THEN 125000.00

        WHEN c.id =
            UNHEX('30000000000040008000000000000001')
            THEN 120000.00

        WHEN c.id =
            UNHEX('30000000000040008000000000000002')
            AND s.seat_type = 'VIP'
            THEN 135000.00

        ELSE 130000.00
    END,

    'AVAILABLE',
    NULL,
    NULL,
    0,
    @now,
    @now

FROM showtimes st

JOIN rooms r
    ON r.id = st.room_id

JOIN cinemas c
    ON c.id = r.cinema_id

JOIN seats s
    ON s.room_id = r.id

WHERE st.id IN (
    UNHEX('50000000000040008000000000000001'),
    UNHEX('50000000000040008000000000000002'),
    UNHEX('50000000000040008000000000000003'),
    UNHEX('50000000000040008000000000000004'),
    UNHEX('50000000000040008000000000000005'),
    UNHEX('50000000000040008000000000000006'),
    UNHEX('50000000000040008000000000000007'),
    UNHEX('50000000000040008000000000000011'),
    UNHEX('50000000000040008000000000000012'),
    UNHEX('50000000000040008000000000000013')
)

ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    status = 'AVAILABLE',
    held_by_booking_id = NULL,
    hold_expires_at = NULL,
    updated_at = VALUES(updated_at);

DROP TEMPORARY TABLE seed_rows;
DROP TEMPORARY TABLE seed_numbers;

COMMIT;
