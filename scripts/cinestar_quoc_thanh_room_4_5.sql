-- CINESTAR QUOC THANH / ROOMS 04 AND 05 / VERIFIED PUBLIC BOOKING SNAPSHOT
-- Generated UTC: 2026-10-05T04:12:47.872667+00:00
-- Backend main: a3fd0fc0fe422e8b655ffdd47a11161b2bb850df
-- Source: https://cinestar.com.vn/movie/628aef99-3632-4449-8968-e59ec91d43a5/
-- Movie: ÁN MẠNG XÉM HOÀN HẢO (T18); source movie ID: 628aef99-3632-4449-8968-e59ec91d43a5
-- Public UI: Ho Chi Minh -> Cinestar Quoc Thanh -> 09/10 -> Standard.
-- Room 04: 2026-10-09 21:00+07:00; observed UTC 2026-10-05T04:08:31.549Z.
-- Room 05: 2026-10-09 21:20+07:00; observed UTC 2026-10-05T04:09:05.285Z.
-- Both shows: adult SINGLE 79000 VND; adult COUPLE 168000 VND per two-person seat unit.
-- Room 04: 181 single + 7 couple = 188 seat units; blocked A06,A07,G09,G10.
-- Room 05: 254 single + 6 couple = 260 seat units; blocked L15,L16.
-- This supplement imports TWO verified rooms and TWO verified showtimes, not all Cinestar inventory.
-- Run after cinestar_seed.sql. Flyway must have created the tables first.
-- Independent of room06 supplement; room06 and existing bookings/holds are not modified.
-- MySQL 8.0.16+; one connection; stop on any error; never use --force.
-- Default database cinema_inventory_db: adjust the USE below if necessary.
-- Local development snapshot; not a live Cinestar availability feed.
-- Source booked cells become UNAVAILABLE; no fictional booking/account is created.
-- Source free cells become AVAILABLE for this local snapshot only.
-- Reruns insert missing rows; existing seats, prices, holds and booking states are preserved.
-- Guards reject unexpected base IDs, layouts or price snapshots; do not remove the guards.
-- Only complete, future SCHEDULED showtimes can become OPEN_FOR_BOOKING.
-- No reopening CLOSED/CANCELLED/COMPLETED; no shifting dates after the show has expired.
-- Single-seat visible legend Ghe Thuong -> STANDARD; two-person seats -> COUPLE.
-- CSS seat-vip regions are preserved in comments; no unverified VIP price is invented.
-- Observed adult prices are explicit; do not regenerate them using SeatPricingPolicy multipliers.
-- Student/senior discounts, promotions, membership and age-rating contracts are not added.
-- Current seats schema lacks layout-column/aisle fields; source columns are comments/TEMP data only.
-- ends_at remains starts_at + source runtime 124 minutes, not an official Cinestar end time.
-- No persistent schema/API changes; max 448 seats + 448 show_seats + 2 opened showtimes.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @cinestar_qt_previous_timezone = @@SESSION.time_zone;
SET SESSION time_zone = '+00:00';
SET @cinestar_qt_now = UTC_TIMESTAMP(6);
USE cinema_inventory_db;
DROP TEMPORARY TABLE IF EXISTS cinestar_qt_guard;
DROP TEMPORARY TABLE IF EXISTS cinestar_qt_rooms;
DROP TEMPORARY TABLE IF EXISTS cinestar_qt_shows;
DROP TEMPORARY TABLE IF EXISTS cinestar_qt_layout;
DROP TEMPORARY TABLE IF EXISTS cinestar_qt_prices;
CREATE TEMPORARY TABLE cinestar_qt_guard (guard_name VARCHAR(100) PRIMARY KEY, ok INT NOT NULL, CHECK (ok = 1)) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_qt_rooms (id BINARY(16) PRIMARY KEY, name VARCHAR(100) NOT NULL, seat_count INT NOT NULL) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_qt_shows (id BINARY(16) PRIMARY KEY, room_id BINARY(16) NOT NULL, starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL, seat_count INT NOT NULL) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_qt_layout (id BINARY(16) PRIMARY KEY, room_id BINARY(16) NOT NULL, seat_number VARCHAR(20) NOT NULL, row_label VARCHAR(10) NOT NULL, seat_type VARCHAR(50) NOT NULL, source_column INT NOT NULL, UNIQUE(room_id,seat_number)) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_qt_prices (id BINARY(16) PRIMARY KEY, showtime_id BINARY(16) NOT NULL, room_id BINARY(16) NOT NULL, seat_number VARCHAR(20) NOT NULL, price DECIMAL(12,2) NOT NULL, snapshot_status VARCHAR(30) NOT NULL, UNIQUE(showtime_id,seat_number)) ENGINE=InnoDB;

INSERT INTO cinestar_qt_rooms (id, name, seat_count) VALUES
    (UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), '04', 188),
    (UNHEX('75175e44310356b697054e8ff8d55d05'), '05', 260);

INSERT INTO cinestar_qt_shows (id, room_id, starts_at, ends_at, seat_count) VALUES
    (UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), '2026-10-09 14:00:00', '2026-10-09 16:04:00', 188),
    (UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), '2026-10-09 14:20:00', '2026-10-09 16:24:00', 260);

INSERT INTO cinestar_qt_layout (id, room_id, seat_number, row_label, seat_type, source_column) VALUES
    -- Room 04 / A01: source column 6
    (UNHEX('0685f69bda8b5863ba63cad5e2afd33f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A01', 'A', 'STANDARD', 6),
    -- Room 04 / A02: source column 7
    (UNHEX('4a33f1990d8f5aaf83977b97a073f495'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A02', 'A', 'STANDARD', 7),
    -- Room 04 / A03: source column 8
    (UNHEX('7b61f458ac2e5401993d3c3d2f98ee01'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A03', 'A', 'STANDARD', 8),
    -- Room 04 / A04: source column 9
    (UNHEX('b1e71d5347ea5595a4de134ab9127f9f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A04', 'A', 'STANDARD', 9),
    -- Room 04 / A05: source column 10
    (UNHEX('8a109872a31e5298842c157b7cf91d78'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A05', 'A', 'STANDARD', 10),
    -- Room 04 / A06: source column 11
    (UNHEX('8c5ca5348abc515b99af6fe392037337'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A06', 'A', 'STANDARD', 11),
    -- Room 04 / A07: source column 12
    (UNHEX('2d8bd809145b5bdca9d6fb2fba897245'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A07', 'A', 'STANDARD', 12),
    -- Room 04 / A08: source column 13
    (UNHEX('098f8f1337b35b17985630256941fd4d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A08', 'A', 'STANDARD', 13),
    -- Room 04 / A09: source column 14
    (UNHEX('93b6cbae79465d4886d4bfc1eb5d68b8'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A09', 'A', 'STANDARD', 14),
    -- Room 04 / A10: source column 15
    (UNHEX('0d7faed43c24501a89a31de939a20da8'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A10', 'A', 'STANDARD', 15),
    -- Room 04 / A11: source column 16
    (UNHEX('1271de043a0a5abcbe632430bb44a065'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A11', 'A', 'STANDARD', 16),
    -- Room 04 / A12: source column 17
    (UNHEX('7203e33970d85bfa95ea87e0db32f13d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A12', 'A', 'STANDARD', 17),
    -- Room 04 / A13: source column 18
    (UNHEX('ae71a377216d5467b1ab26036ed967b0'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A13', 'A', 'STANDARD', 18),
    -- Room 04 / A14: source column 19
    (UNHEX('2882beb48d945ff2aea5a335b5dc1931'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A14', 'A', 'STANDARD', 19),
    -- Room 04 / B01: source column 6
    (UNHEX('d6c1e584279f51ee96cbf688ae7fc2c7'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B01', 'B', 'STANDARD', 6),
    -- Room 04 / B02: source column 7
    (UNHEX('c439fd4c24ec578ebed04fc54c8d7d51'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B02', 'B', 'STANDARD', 7),
    -- Room 04 / B03: source column 8
    (UNHEX('ed3ddb45f2d75663a62a0646b12d17a5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B03', 'B', 'STANDARD', 8),
    -- Room 04 / B04: source column 9
    (UNHEX('6118aa78a77352f5baea434fc3a22fa0'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B04', 'B', 'STANDARD', 9),
    -- Room 04 / B05: source column 10
    (UNHEX('c57e1bae14f8567b8fe7d5cd4a8edbfd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B05', 'B', 'STANDARD', 10),
    -- Room 04 / B06: source column 11
    (UNHEX('3e7a8a5536685d5b9caeb7e7eb46713c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B06', 'B', 'STANDARD', 11),
    -- Room 04 / B07: source column 12
    (UNHEX('9b6cb34af2da5e118cd8ec02bef28e8f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B07', 'B', 'STANDARD', 12),
    -- Room 04 / B08: source column 13
    (UNHEX('4147f2a00ac95d32b16308ba9f9a732a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B08', 'B', 'STANDARD', 13),
    -- Room 04 / B09: source column 14
    (UNHEX('108159b66afb52469e27902078774192'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B09', 'B', 'STANDARD', 14),
    -- Room 04 / B10: source column 15
    (UNHEX('b1096209733859b9bf745e82783e6496'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B10', 'B', 'STANDARD', 15),
    -- Room 04 / B11: source column 16
    (UNHEX('46392255c55d5607a39fc94077786520'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B11', 'B', 'STANDARD', 16),
    -- Room 04 / B12: source column 17
    (UNHEX('366b9a33f0705135b0c9aea4da824f27'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B12', 'B', 'STANDARD', 17),
    -- Room 04 / B13: source column 18
    (UNHEX('d1d41e87576f5e08aedc67d4c555533c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B13', 'B', 'STANDARD', 18),
    -- Room 04 / B14: source column 19
    (UNHEX('b6e23f86eb6854c399a3480fb3683310'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B14', 'B', 'STANDARD', 19),
    -- Room 04 / C01: source column 3
    (UNHEX('615959e285cd5354a6310629ff4c4d69'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C01', 'C', 'STANDARD', 3),
    -- Room 04 / C02: source column 4
    (UNHEX('d1cedb64cd4c5cc8bcd670107f9cab99'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C02', 'C', 'STANDARD', 4),
    -- Room 04 / C03: source column 5
    (UNHEX('911d2c469e3150d4b1c3f04810267aa9'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C03', 'C', 'STANDARD', 5),
    -- Room 04 / C04: source column 6
    (UNHEX('4f7fbd90e9b057f18715a801a02116e4'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C04', 'C', 'STANDARD', 6),
    -- Room 04 / C05: source column 7
    (UNHEX('87aa807d447c515aba21bbc25cd9072f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C05', 'C', 'STANDARD', 7),
    -- Room 04 / C06: source column 8
    (UNHEX('d47c98a0884e5d76826ea8a77e74cfce'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C06', 'C', 'STANDARD', 8),
    -- Room 04 / C07: source column 9
    (UNHEX('4c03a569a8615589bd5e1ab822338770'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C07', 'C', 'STANDARD', 9),
    -- Room 04 / C08: source column 10
    (UNHEX('6099929b09ff556e9fb731ebff2d8a72'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C08', 'C', 'STANDARD', 10),
    -- Room 04 / C09: source column 11
    (UNHEX('848e524e992e5df6b01064e90daa7794'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C09', 'C', 'STANDARD', 11),
    -- Room 04 / C10: source column 12
    (UNHEX('ba146ad3b2405b448a12951802d0adc6'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C10', 'C', 'STANDARD', 12),
    -- Room 04 / C11: source column 13
    (UNHEX('714b25d0d0eb5146b14f0752b684eb63'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C11', 'C', 'STANDARD', 13),
    -- Room 04 / C12: source column 14
    (UNHEX('521e4f87c2655d8d87b43aa1a32b82da'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C12', 'C', 'STANDARD', 14),
    -- Room 04 / C13: source column 15
    (UNHEX('7ca9a26ef2fe558ab0af856db42f85de'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C13', 'C', 'STANDARD', 15),
    -- Room 04 / C14: source column 16
    (UNHEX('357d0df15f4858ffbc05e294c499addf'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C14', 'C', 'STANDARD', 16),
    -- Room 04 / C15: source column 17
    (UNHEX('a9dbdc9b2a735285b577dedd88f80fb0'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C15', 'C', 'STANDARD', 17),
    -- Room 04 / C16: source column 18
    (UNHEX('fe434426f74157839fca714d033348fb'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C16', 'C', 'STANDARD', 18),
    -- Room 04 / C17: source column 19
    (UNHEX('1fb192205905518e9aee36bf19ab2cb8'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C17', 'C', 'STANDARD', 19),
    -- Room 04 / D01: source column 3
    (UNHEX('25439ac62c9c53ef8640d4b483c1694b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D01', 'D', 'STANDARD', 3),
    -- Room 04 / D02: source column 4
    (UNHEX('06159a42cc9c5ac9b6a04e2e93303696'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D02', 'D', 'STANDARD', 4),
    -- Room 04 / D03: source column 5
    (UNHEX('1343845bdf835cf6a01d0e5d073872d5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D03', 'D', 'STANDARD', 5),
    -- Room 04 / D04: source column 6
    (UNHEX('26e65414d425583daf2bf310e2ea55ec'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D04', 'D', 'STANDARD', 6),
    -- Room 04 / D05: source column 7
    (UNHEX('0662f6e22629563fbeefad0b6e43f3cb'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D05', 'D', 'STANDARD', 7),
    -- Room 04 / D06: source column 8
    (UNHEX('37ea609939dc5bb89fa65346f16a03b3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D06', 'D', 'STANDARD', 8),
    -- Room 04 / D07: source column 9
    (UNHEX('3ae2da766dea56da91e10ae1d6dafdd2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D07', 'D', 'STANDARD', 9),
    -- Room 04 / D08: source column 10
    (UNHEX('e557dd591b37563b895761841bc75d0b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D08', 'D', 'STANDARD', 10),
    -- Room 04 / D09: source column 11
    (UNHEX('167d19290f9858a286c8103ca3d56c5f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D09', 'D', 'STANDARD', 11),
    -- Room 04 / D10: source column 12
    (UNHEX('63f39d4d3795541e9532ea8a5d545a45'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D10', 'D', 'STANDARD', 12),
    -- Room 04 / D11: source column 13
    (UNHEX('cb41848642b757c4b6fd64afda187829'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D11', 'D', 'STANDARD', 13),
    -- Room 04 / D12: source column 14
    (UNHEX('7202e47c3db05a0d9eaf3f202362f91b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D12', 'D', 'STANDARD', 14),
    -- Room 04 / D13: source column 15
    (UNHEX('2c546182377251ec882fc0cc8e09b4d4'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D13', 'D', 'STANDARD', 15),
    -- Room 04 / D14: source column 16
    (UNHEX('fbf97d8a5e675e51b927d96791afaf5d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D14', 'D', 'STANDARD', 16),
    -- Room 04 / D15: source column 17
    (UNHEX('bccb835a92215589ad261331714bbe5a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D15', 'D', 'STANDARD', 17),
    -- Room 04 / D16: source column 18
    (UNHEX('057ffdb83d59560e8067efea4ff8b6e9'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D16', 'D', 'STANDARD', 18),
    -- Room 04 / D17: source column 19
    (UNHEX('f745cbd2e9a250779918217f386acb21'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D17', 'D', 'STANDARD', 19),
    -- Room 04 / E01: source column 3
    (UNHEX('8cd26754e55d58a2829ddee8742361cc'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E01', 'E', 'STANDARD', 3),
    -- Room 04 / E02: source column 4
    (UNHEX('cec2e0435aaf543aad77ce7708f06547'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E02', 'E', 'STANDARD', 4),
    -- Room 04 / E03: source column 5
    (UNHEX('c10afbac1e245c84aab880d5dd4c0b1a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E03', 'E', 'STANDARD', 5),
    -- Room 04 / E04: source column 6
    (UNHEX('5a936f9a725554cb93a162fc2447f299'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E04', 'E', 'STANDARD', 6),
    -- Room 04 / E05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('ba297c1dc4c252ed9bb1fb24f59bbcd3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E05', 'E', 'STANDARD', 7),
    -- Room 04 / E06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('181d5e7c344c52529ebc7b4f4f6e7fc1'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E06', 'E', 'STANDARD', 8),
    -- Room 04 / E07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('f964320d89885d0898752d721bef0437'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E07', 'E', 'STANDARD', 9),
    -- Room 04 / E08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('644cda33ada35b148da882efe50c8f41'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E08', 'E', 'STANDARD', 10),
    -- Room 04 / E09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('960297b380855fbf810c14db1cf9b5c5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E09', 'E', 'STANDARD', 11),
    -- Room 04 / E10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('99e813cd2f235743ba9173c835cb6177'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E10', 'E', 'STANDARD', 12),
    -- Room 04 / E11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('fd8f072cc062560d85a56545a2de79bb'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E11', 'E', 'STANDARD', 13),
    -- Room 04 / E12: source column 14; CSS seat-vip, visible single-seat legend
    (UNHEX('72e36c4a59ac5329967b5bfca0911658'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E12', 'E', 'STANDARD', 14),
    -- Room 04 / E13: source column 15; CSS seat-vip, visible single-seat legend
    (UNHEX('842cede1053e54098fc2f0701bf7f02b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E13', 'E', 'STANDARD', 15),
    -- Room 04 / E14: source column 16
    (UNHEX('1d88bbc12df0514280306de37ff4faeb'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E14', 'E', 'STANDARD', 16),
    -- Room 04 / E15: source column 17
    (UNHEX('7f3c7f4fe8345789ad31090acaf3f91b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E15', 'E', 'STANDARD', 17),
    -- Room 04 / E16: source column 18
    (UNHEX('9e3d786d2eb85cad8fed797fd2e2503a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E16', 'E', 'STANDARD', 18),
    -- Room 04 / E17: source column 19
    (UNHEX('b40d23e2984a5ff7b0b36b58330a72e9'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E17', 'E', 'STANDARD', 19),
    -- Room 04 / F01: source column 3
    (UNHEX('4f6081ffb7b151e6b0cbb0f84e49de5a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F01', 'F', 'STANDARD', 3),
    -- Room 04 / F02: source column 4
    (UNHEX('40b8e9f402985dbe820f4ed3ea67d027'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F02', 'F', 'STANDARD', 4),
    -- Room 04 / F03: source column 5
    (UNHEX('6533e28742295834a56b6f86b4cc8eb5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F03', 'F', 'STANDARD', 5),
    -- Room 04 / F04: source column 6
    (UNHEX('7d1f049dcb4959f1a91b2a6b5cc7ab41'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F04', 'F', 'STANDARD', 6),
    -- Room 04 / F05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('2d27977956395587bc40bf0b7b453279'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F05', 'F', 'STANDARD', 7),
    -- Room 04 / F06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('6e09290f807a59419d03fd05ac2a78fd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F06', 'F', 'STANDARD', 8),
    -- Room 04 / F07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('614422ba972156e3b9f88100a3709b9a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F07', 'F', 'STANDARD', 9),
    -- Room 04 / F08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('c0e4278559b65544bca07ce10800df3b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F08', 'F', 'STANDARD', 10),
    -- Room 04 / F09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('40ec6ec4d83653909e768085ed323071'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F09', 'F', 'STANDARD', 11),
    -- Room 04 / F10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('a7926c949ab9591aa25b6aa36bf78d4d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F10', 'F', 'STANDARD', 12),
    -- Room 04 / F11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('cac46d24c7d15ec780482ce1602fd4af'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F11', 'F', 'STANDARD', 13),
    -- Room 04 / F12: source column 14; CSS seat-vip, visible single-seat legend
    (UNHEX('957cae3ec2c5595284a351fd0dfa7144'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F12', 'F', 'STANDARD', 14),
    -- Room 04 / F13: source column 15; CSS seat-vip, visible single-seat legend
    (UNHEX('3471b2473e0653928523ade4e5ee35dd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F13', 'F', 'STANDARD', 15),
    -- Room 04 / F14: source column 16
    (UNHEX('016c6fc034e1538a87526657da42a665'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F14', 'F', 'STANDARD', 16),
    -- Room 04 / F15: source column 17
    (UNHEX('f2c0c82e82a95cf4808a493212859381'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F15', 'F', 'STANDARD', 17),
    -- Room 04 / F16: source column 18
    (UNHEX('b05133c3db3d5f2485a7eebb93b3d9c5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F16', 'F', 'STANDARD', 18),
    -- Room 04 / F17: source column 19
    (UNHEX('7a2d570774c55c288ebc731596e1123d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F17', 'F', 'STANDARD', 19),
    -- Room 04 / G01: source column 3
    (UNHEX('62a0380060875a4e96ff454c8b4403bc'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G01', 'G', 'STANDARD', 3),
    -- Room 04 / G02: source column 4
    (UNHEX('9166c8ee3f465f14a759311e6f45d44f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G02', 'G', 'STANDARD', 4),
    -- Room 04 / G03: source column 5
    (UNHEX('68b25891996f5f58b1aaf0adedfe39a5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G03', 'G', 'STANDARD', 5),
    -- Room 04 / G04: source column 6
    (UNHEX('ddb30a257ee05271a48b7e4dac1fb555'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G04', 'G', 'STANDARD', 6),
    -- Room 04 / G05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('f61d3b31398356c688cf7bfbb30aa4c3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G05', 'G', 'STANDARD', 7),
    -- Room 04 / G06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('fcda19c4c80c575fb57100b426a26e4e'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G06', 'G', 'STANDARD', 8),
    -- Room 04 / G07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('b55196a60a135afa94a042bec652b72f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G07', 'G', 'STANDARD', 9),
    -- Room 04 / G08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('1e3dfc2d2a865f55b61d68d5aa701393'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G08', 'G', 'STANDARD', 10),
    -- Room 04 / G09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('469e452177ec575394555881960ea287'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G09', 'G', 'STANDARD', 11),
    -- Room 04 / G10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('fcc0649f44e55048920650c8b66c8907'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G10', 'G', 'STANDARD', 12),
    -- Room 04 / G11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('3d84312b331150e8b04aa532608e57e9'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G11', 'G', 'STANDARD', 13),
    -- Room 04 / G12: source column 14; CSS seat-vip, visible single-seat legend
    (UNHEX('121f1449c866526aaf4e64c444de479f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G12', 'G', 'STANDARD', 14),
    -- Room 04 / G13: source column 15; CSS seat-vip, visible single-seat legend
    (UNHEX('35b9443f3ef450168e914f8e8eb29fa4'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G13', 'G', 'STANDARD', 15),
    -- Room 04 / G14: source column 16
    (UNHEX('86a7c3ebcefa5c36b0255c1b69497a7f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G14', 'G', 'STANDARD', 16),
    -- Room 04 / G15: source column 17
    (UNHEX('4ab04ac707d8580da830df50e72b6925'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G15', 'G', 'STANDARD', 17),
    -- Room 04 / G16: source column 18
    (UNHEX('77af2ade0194568786965a3a4871ef4e'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G16', 'G', 'STANDARD', 18),
    -- Room 04 / G17: source column 19
    (UNHEX('402fdbc0d2635831908bfb818f82e1fa'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G17', 'G', 'STANDARD', 19),
    -- Room 04 / H01: source column 3
    (UNHEX('e0dcecdf5aca525aa07648545dc67949'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H01', 'H', 'STANDARD', 3),
    -- Room 04 / H02: source column 4
    (UNHEX('c5e7f6c9e7575eb6bc1e7650dbc65dff'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H02', 'H', 'STANDARD', 4),
    -- Room 04 / H03: source column 5
    (UNHEX('e74f2cdf6c165d4994ea2e521f6ce2d6'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H03', 'H', 'STANDARD', 5),
    -- Room 04 / H04: source column 6
    (UNHEX('4fe36745cbe153b9b8a552792472a80b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H04', 'H', 'STANDARD', 6),
    -- Room 04 / H05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('b1ce5f98061953c6835778a93d1c480f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H05', 'H', 'STANDARD', 7),
    -- Room 04 / H06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('ed29fe97abbc5961a526bf3c5aa547e8'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H06', 'H', 'STANDARD', 8),
    -- Room 04 / H07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('e753899b273a5559ba3e77bdaedbd2b2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H07', 'H', 'STANDARD', 9),
    -- Room 04 / H08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('c0a1e83ddbff58eb94038ffbffb04b33'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H08', 'H', 'STANDARD', 10),
    -- Room 04 / H09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('3e4541322d2c55898c1d06dd5a16c4a7'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H09', 'H', 'STANDARD', 11),
    -- Room 04 / H10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('987fb650123a5adf89379f58927f47a1'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H10', 'H', 'STANDARD', 12),
    -- Room 04 / H11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('d525f4c75e8d5a05be2970470095f095'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H11', 'H', 'STANDARD', 13),
    -- Room 04 / H12: source column 14; CSS seat-vip, visible single-seat legend
    (UNHEX('378c67f468b35b97b04e175f9301afd3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H12', 'H', 'STANDARD', 14),
    -- Room 04 / H13: source column 15; CSS seat-vip, visible single-seat legend
    (UNHEX('ee74ca846b365afe9b2d9bb6b00a2d75'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H13', 'H', 'STANDARD', 15),
    -- Room 04 / H14: source column 16
    (UNHEX('f7a86d0fa307540799bda6311ce6bef2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H14', 'H', 'STANDARD', 16),
    -- Room 04 / H15: source column 17
    (UNHEX('becc9bf6621d5eb484ea0a026f68f69f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H15', 'H', 'STANDARD', 17),
    -- Room 04 / H16: source column 18
    (UNHEX('e73083ccb9185388abc27b9a7f9087e6'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H16', 'H', 'STANDARD', 18),
    -- Room 04 / H17: source column 19
    (UNHEX('71dcb2b7005b573e97b01526e3334751'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H17', 'H', 'STANDARD', 19),
    -- Room 04 / J01: source column 3
    (UNHEX('9343591263e4521d994b1f9fb518cf85'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J01', 'J', 'STANDARD', 3),
    -- Room 04 / J02: source column 4
    (UNHEX('dbf120d9f76554a7ba91bf666022b49a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J02', 'J', 'STANDARD', 4),
    -- Room 04 / J03: source column 5
    (UNHEX('20f51c93eae15051b8f5028af1002e74'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J03', 'J', 'STANDARD', 5),
    -- Room 04 / J04: source column 6
    (UNHEX('a997db2c06885269ac8f403dd6b7da60'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J04', 'J', 'STANDARD', 6),
    -- Room 04 / J05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('f9f404f1cef85b9d936076fcf11f5327'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J05', 'J', 'STANDARD', 7),
    -- Room 04 / J06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('e29a72d3184656cc905a83bfcc5dba14'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J06', 'J', 'STANDARD', 8),
    -- Room 04 / J07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('0569e714ad2a5e21bb8ad6ac9017c007'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J07', 'J', 'STANDARD', 9),
    -- Room 04 / J08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('cea604b13821535595039a69c6ab8581'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J08', 'J', 'STANDARD', 10),
    -- Room 04 / J09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('44fdd50244425b1a89ed34d057122e87'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J09', 'J', 'STANDARD', 11),
    -- Room 04 / J10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('994e3183920657838e3bf23493bcd880'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J10', 'J', 'STANDARD', 12),
    -- Room 04 / J11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('5b3116dc171659388a0508348687888d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J11', 'J', 'STANDARD', 13),
    -- Room 04 / J12: source column 14; CSS seat-vip, visible single-seat legend
    (UNHEX('42131ee2af8d5fb4bc02a0a434b1fd0e'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J12', 'J', 'STANDARD', 14),
    -- Room 04 / J13: source column 15; CSS seat-vip, visible single-seat legend
    (UNHEX('b7fee3f5f72753de92659ecad95a91a7'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J13', 'J', 'STANDARD', 15),
    -- Room 04 / J14: source column 16
    (UNHEX('96b61a36414d57eb82e8d79870583e29'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J14', 'J', 'STANDARD', 16),
    -- Room 04 / J15: source column 17
    (UNHEX('4f858d9c79845b448e8f77b3f91a60ac'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J15', 'J', 'STANDARD', 17),
    -- Room 04 / J16: source column 18
    (UNHEX('5df769038fe153889f383502001aadd2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J16', 'J', 'STANDARD', 18),
    -- Room 04 / J17: source column 19
    (UNHEX('99519c1d756f56eca128151c3305afd3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J17', 'J', 'STANDARD', 19),
    -- Room 04 / K01: source column 3
    (UNHEX('e2d6c39783405d218422c40529bdd544'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K01', 'K', 'STANDARD', 3),
    -- Room 04 / K02: source column 4
    (UNHEX('9755d6e4e61a5c96bb514cafbd4fe6c1'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K02', 'K', 'STANDARD', 4),
    -- Room 04 / K03: source column 5
    (UNHEX('33d73c8b5b395a878c9e36ceb7a4e1e0'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K03', 'K', 'STANDARD', 5),
    -- Room 04 / K04: source column 6
    (UNHEX('3eaa885bf0335916a1e73faaeda2878b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K04', 'K', 'STANDARD', 6),
    -- Room 04 / K05: source column 7
    (UNHEX('963a382bf5e351648abdfdebc4b2392b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K05', 'K', 'STANDARD', 7),
    -- Room 04 / K06: source column 8
    (UNHEX('3578f5cd93fc5912bc8d6623469c5be7'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K06', 'K', 'STANDARD', 8),
    -- Room 04 / K07: source column 9
    (UNHEX('b754a32570255ec6958a1f731238d0cc'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K07', 'K', 'STANDARD', 9),
    -- Room 04 / K08: source column 10
    (UNHEX('ab18f6bdfcc35d9f96128093f445e6c8'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K08', 'K', 'STANDARD', 10),
    -- Room 04 / K09: source column 11
    (UNHEX('e9d3038f6808549aad3ae61c8b52aa05'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K09', 'K', 'STANDARD', 11),
    -- Room 04 / K10: source column 12
    (UNHEX('8d8d21c729065ecbba64bd41720b2efa'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K10', 'K', 'STANDARD', 12),
    -- Room 04 / K11: source column 13
    (UNHEX('4c397e4424bf514ba3850b764e91377b'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K11', 'K', 'STANDARD', 13),
    -- Room 04 / K12: source column 14
    (UNHEX('51d0e2407459545c9801351820581461'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K12', 'K', 'STANDARD', 14),
    -- Room 04 / K13: source column 15
    (UNHEX('d3eb03410799503a9a5ec22c3b257ed0'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K13', 'K', 'STANDARD', 15),
    -- Room 04 / K14: source column 16
    (UNHEX('78741ee160f459ac80928385ffb3b315'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K14', 'K', 'STANDARD', 16),
    -- Room 04 / K15: source column 17
    (UNHEX('a14ae98c6e1954c49a275b0367a615fd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K15', 'K', 'STANDARD', 17),
    -- Room 04 / K16: source column 18
    (UNHEX('e8f3281e06295b93bb52a5a0b042eda9'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K16', 'K', 'STANDARD', 18),
    -- Room 04 / K17: source column 19
    (UNHEX('cc4a43d173c55e87a30e87d913d433c3'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K17', 'K', 'STANDARD', 19),
    -- Room 04 / L01: source column 3
    (UNHEX('572db98d9467537a8d310dff147fe91c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L01', 'L', 'STANDARD', 3),
    -- Room 04 / L02: source column 4
    (UNHEX('38d524116fa151889e73cf56086713ac'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L02', 'L', 'STANDARD', 4),
    -- Room 04 / L03: source column 5
    (UNHEX('cee8680ea3ab533d9169ab739efd0c06'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L03', 'L', 'STANDARD', 5),
    -- Room 04 / L04: source column 6
    (UNHEX('7a1de3c7fcd55732b95cf5c4e9c30fdf'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L04', 'L', 'STANDARD', 6),
    -- Room 04 / L05: source column 7
    (UNHEX('4f9147622b9e5612bb918ca9444570bd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L05', 'L', 'STANDARD', 7),
    -- Room 04 / L06: source column 8
    (UNHEX('d401dd181e1d5387986287584706d213'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L06', 'L', 'STANDARD', 8),
    -- Room 04 / L07: source column 9
    (UNHEX('731402654ada5a6ea780e06638bd415a'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L07', 'L', 'STANDARD', 9),
    -- Room 04 / L08: source column 10
    (UNHEX('116f638b74925ab1a012aba30716cdb5'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L08', 'L', 'STANDARD', 10),
    -- Room 04 / L09: source column 11
    (UNHEX('7b2066c3998854af825cd8c3961139b1'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L09', 'L', 'STANDARD', 11),
    -- Room 04 / L10: source column 12
    (UNHEX('1559c5225b3c5ac8b21d202d8c81e88c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L10', 'L', 'STANDARD', 12),
    -- Room 04 / L11: source column 13
    (UNHEX('75b65b3817b15587befa631029f523ea'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L11', 'L', 'STANDARD', 13),
    -- Room 04 / L12: source column 14
    (UNHEX('2073d17392055fc39f3fcd5a0dccf2ed'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L12', 'L', 'STANDARD', 14),
    -- Room 04 / L13: source column 15
    (UNHEX('54874a116366558987bf3768c71d328c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L13', 'L', 'STANDARD', 15),
    -- Room 04 / L14: source column 16
    (UNHEX('33ec2432cf1158e88b8e5bc267fec2e2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L14', 'L', 'STANDARD', 16),
    -- Room 04 / L15: source column 17
    (UNHEX('5ec64fb4081850199a90a7c7d9fed330'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L15', 'L', 'STANDARD', 17),
    -- Room 04 / L16: source column 18
    (UNHEX('0a44348617495b40aba7c2c2a57ed97f'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L16', 'L', 'STANDARD', 18),
    -- Room 04 / L17: source column 19
    (UNHEX('924cf80ec556518b9bd9b5f293c6eac1'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L17', 'L', 'STANDARD', 19),
    -- Room 04 / M01: source column 1
    (UNHEX('abcefe853b0f5404ad80c4ab7f8bc16e'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M01', 'M', 'COUPLE', 1),
    -- Room 04 / M02: source column 2
    (UNHEX('6959ce1182c3591c947b6bed22ff040d'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M02', 'M', 'COUPLE', 2),
    -- Room 04 / M03: source column 3
    (UNHEX('f5dc076b76055d74b88b142b8637ec62'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M03', 'M', 'COUPLE', 3),
    -- Room 04 / M04: source column 4
    (UNHEX('d859fc676d09537ebe8b3cde6dc533c2'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M04', 'M', 'COUPLE', 4),
    -- Room 04 / M05: source column 12
    (UNHEX('c1fb6d1fab445338a36cd912c272f3ae'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M05', 'M', 'COUPLE', 12),
    -- Room 04 / M06: source column 13
    (UNHEX('d09f6c51559753bcb5c724d3b1f641fd'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M06', 'M', 'COUPLE', 13),
    -- Room 04 / M07: source column 14
    (UNHEX('392c9cff0af3561bb9268f1b384c4614'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M07', 'M', 'COUPLE', 14),
    -- Room 05 / A01: source column 5
    (UNHEX('e1fd07fa976a582fbb36f3c215d99716'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A01', 'A', 'STANDARD', 5),
    -- Room 05 / A02: source column 6
    (UNHEX('844244b6d3135495858db25d0af012e7'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A02', 'A', 'STANDARD', 6),
    -- Room 05 / A03: source column 7
    (UNHEX('ccf8941ac7be5da9a5fe3e02ab1cc90f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A03', 'A', 'STANDARD', 7),
    -- Room 05 / A04: source column 8
    (UNHEX('cdade65d6c9a5e56a4728de70c0483e4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A04', 'A', 'STANDARD', 8),
    -- Room 05 / A05: source column 9
    (UNHEX('2df33bcbe788550a8464d3897239ff25'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A05', 'A', 'STANDARD', 9),
    -- Room 05 / A06: source column 10
    (UNHEX('b13076162e6659239c7b39e8c1de48eb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A06', 'A', 'STANDARD', 10),
    -- Room 05 / A07: source column 11
    (UNHEX('00c3425e78e457f6bc5459cc5d78dcbb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A07', 'A', 'STANDARD', 11),
    -- Room 05 / A08: source column 12
    (UNHEX('a7c0b6c8c16454b0b9177d7666c10162'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A08', 'A', 'STANDARD', 12),
    -- Room 05 / A09: source column 13
    (UNHEX('992bfe4d655152b59d88002966d108c3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A09', 'A', 'STANDARD', 13),
    -- Room 05 / A10: source column 14
    (UNHEX('43390da0d9775050a9a08c673c66821a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A10', 'A', 'STANDARD', 14),
    -- Room 05 / A11: source column 15
    (UNHEX('292d76defeea55c289ff286f4ce1dc63'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A11', 'A', 'STANDARD', 15),
    -- Room 05 / A12: source column 16
    (UNHEX('f72815b3a49a59aeb1e561accbd2be83'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A12', 'A', 'STANDARD', 16),
    -- Room 05 / A13: source column 17
    (UNHEX('f000a21c512156d788471d72d3508497'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A13', 'A', 'STANDARD', 17),
    -- Room 05 / A14: source column 18
    (UNHEX('4da68b345c5052a19635f7f6c9a8cae1'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A14', 'A', 'STANDARD', 18),
    -- Room 05 / A15: source column 19
    (UNHEX('349544e256d557018dc6cdf8723458bb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A15', 'A', 'STANDARD', 19),
    -- Room 05 / B01: source column 5
    (UNHEX('35f93f075593577eb56acf192f52814b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B01', 'B', 'STANDARD', 5),
    -- Room 05 / B02: source column 6
    (UNHEX('4e8ea98dad085fa4a185d6e090469d41'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B02', 'B', 'STANDARD', 6),
    -- Room 05 / B03: source column 7
    (UNHEX('7d4158c807295ead919e004f5778b3b0'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B03', 'B', 'STANDARD', 7),
    -- Room 05 / B04: source column 8
    (UNHEX('17e17ff9afb7517f9a89b7c91cfaf215'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B04', 'B', 'STANDARD', 8),
    -- Room 05 / B05: source column 9
    (UNHEX('f42609ca26135b6da4c2aac6248017a6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B05', 'B', 'STANDARD', 9),
    -- Room 05 / B06: source column 10
    (UNHEX('3a1797f649c55773a740500cfee14691'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B06', 'B', 'STANDARD', 10),
    -- Room 05 / B07: source column 11
    (UNHEX('d2fc64bea1df52519e6407540bbfb17b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B07', 'B', 'STANDARD', 11),
    -- Room 05 / B08: source column 12
    (UNHEX('4d2ea206764654f087272037117da884'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B08', 'B', 'STANDARD', 12),
    -- Room 05 / B09: source column 13
    (UNHEX('93931dc2fd225018bc996162c2f809e8'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B09', 'B', 'STANDARD', 13),
    -- Room 05 / B10: source column 14
    (UNHEX('ebe2bfd63b2f5e86a46b183292dd72b6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B10', 'B', 'STANDARD', 14),
    -- Room 05 / B11: source column 15
    (UNHEX('719e4611297950cc9691400dab18b427'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B11', 'B', 'STANDARD', 15),
    -- Room 05 / B12: source column 16
    (UNHEX('df909d25545653619a116c87d6928e8d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B12', 'B', 'STANDARD', 16),
    -- Room 05 / B13: source column 17
    (UNHEX('1944b560ad505fd6b3ca70ae4c3e527e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B13', 'B', 'STANDARD', 17),
    -- Room 05 / B14: source column 18
    (UNHEX('07ea51f0a2b55842b55904212bdce833'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B14', 'B', 'STANDARD', 18),
    -- Room 05 / B15: source column 19
    (UNHEX('5631b5c5e59c5786906ba1d11e7b9fc2'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B15', 'B', 'STANDARD', 19),
    -- Room 05 / C01: source column 5
    (UNHEX('85f9313d9300570384358ba40c2877ff'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C01', 'C', 'STANDARD', 5),
    -- Room 05 / C02: source column 6
    (UNHEX('01edc077bcbe5715857f6ed19418850c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C02', 'C', 'STANDARD', 6),
    -- Room 05 / C03: source column 7
    (UNHEX('9d59e410fc2255b1b6f89e67007273c9'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C03', 'C', 'STANDARD', 7),
    -- Room 05 / C04: source column 8
    (UNHEX('4280634746a658c18ecce489a43c8b0a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C04', 'C', 'STANDARD', 8),
    -- Room 05 / C05: source column 9
    (UNHEX('ed3b3a0157355e39933e0cd9b8a71092'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C05', 'C', 'STANDARD', 9),
    -- Room 05 / C06: source column 10
    (UNHEX('b9007ded1e395148838683b1104206e2'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C06', 'C', 'STANDARD', 10),
    -- Room 05 / C07: source column 11
    (UNHEX('53e24c1e12cd55ef987ffbae88410cff'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C07', 'C', 'STANDARD', 11),
    -- Room 05 / C08: source column 12
    (UNHEX('1e5bbe6239395fa59b5f42a9d0976327'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C08', 'C', 'STANDARD', 12),
    -- Room 05 / C09: source column 13
    (UNHEX('4af99d7ec6925881bf496ddc1819bf48'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C09', 'C', 'STANDARD', 13),
    -- Room 05 / C10: source column 14
    (UNHEX('87b05140c0105a99b5cfc970b5e7ab99'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C10', 'C', 'STANDARD', 14),
    -- Room 05 / C11: source column 15
    (UNHEX('31f2eb05cde15e8db4487162bfc7313e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C11', 'C', 'STANDARD', 15),
    -- Room 05 / C12: source column 16
    (UNHEX('e8835c7ac5495efc82867de15bdba432'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C12', 'C', 'STANDARD', 16),
    -- Room 05 / C13: source column 17
    (UNHEX('84eb892b827f5bfa80a7d6fd49a353ea'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C13', 'C', 'STANDARD', 17),
    -- Room 05 / C14: source column 18
    (UNHEX('962fd7c1221b54279b0984e471cbc951'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C14', 'C', 'STANDARD', 18),
    -- Room 05 / C15: source column 19
    (UNHEX('6744c4fc21755d989bf85090261a06e6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C15', 'C', 'STANDARD', 19),
    -- Room 05 / D01: source column 5
    (UNHEX('2ebd382fccae576dadc86482486c33a7'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D01', 'D', 'STANDARD', 5),
    -- Room 05 / D02: source column 6
    (UNHEX('ed744f0dc8ea518bba3c7a194d6319b8'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D02', 'D', 'STANDARD', 6),
    -- Room 05 / D03: source column 7
    (UNHEX('5764224b660450cdb46fb394a9ad545e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D03', 'D', 'STANDARD', 7),
    -- Room 05 / D04: source column 8
    (UNHEX('04f53bd4f3885cb0a518af725e477a4b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D04', 'D', 'STANDARD', 8),
    -- Room 05 / D05: source column 9
    (UNHEX('5448397c2e7756f5a8f003217714361a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D05', 'D', 'STANDARD', 9),
    -- Room 05 / D06: source column 10
    (UNHEX('73b6de17e32e584693eb8b3c809a43e6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D06', 'D', 'STANDARD', 10),
    -- Room 05 / D07: source column 11
    (UNHEX('9a904815b2845d6abb9680c24122c04d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D07', 'D', 'STANDARD', 11),
    -- Room 05 / D08: source column 12
    (UNHEX('dbf253e27bbe507f80cf7369241e2c70'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D08', 'D', 'STANDARD', 12),
    -- Room 05 / D09: source column 13
    (UNHEX('339d15add6af5e50b0fd1187ff7c78ee'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D09', 'D', 'STANDARD', 13),
    -- Room 05 / D10: source column 14
    (UNHEX('678bada7f7f05fd1b2283a2e30d6689b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D10', 'D', 'STANDARD', 14),
    -- Room 05 / D11: source column 15
    (UNHEX('e418905e3b075b8c9d1a288450b7dc46'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D11', 'D', 'STANDARD', 15),
    -- Room 05 / D12: source column 16
    (UNHEX('39c781aaec1d55f9a276977fd8cd5b0c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D12', 'D', 'STANDARD', 16),
    -- Room 05 / D13: source column 17
    (UNHEX('9406f2f698c1523e9a19f58ee93536ca'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D13', 'D', 'STANDARD', 17),
    -- Room 05 / D14: source column 18
    (UNHEX('9412bcf819715e358947627ec06a739c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D14', 'D', 'STANDARD', 18),
    -- Room 05 / D15: source column 19
    (UNHEX('95756dafb17e583db2aa53fb8082ff5c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D15', 'D', 'STANDARD', 19),
    -- Room 05 / E01: source column 5
    (UNHEX('bae3060973445e81b8157f9686b4da61'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E01', 'E', 'STANDARD', 5),
    -- Room 05 / E02: source column 6
    (UNHEX('24ea1adb3bb1577c8b61e4bd3931fcea'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E02', 'E', 'STANDARD', 6),
    -- Room 05 / E03: source column 7
    (UNHEX('ddb211e1e68f57efbb6581eb9b32a06f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E03', 'E', 'STANDARD', 7),
    -- Room 05 / E04: source column 8
    (UNHEX('9c44642c19d151f9920414596237a2a4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E04', 'E', 'STANDARD', 8),
    -- Room 05 / E05: source column 9
    (UNHEX('0c55ba6d9968561cb51e498825cc9b9d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E05', 'E', 'STANDARD', 9),
    -- Room 05 / E06: source column 10
    (UNHEX('55011488adc3536a8594b22eb8c3900c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E06', 'E', 'STANDARD', 10),
    -- Room 05 / E07: source column 11
    (UNHEX('b98b2dbdafc7566eb3169448d962aab9'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E07', 'E', 'STANDARD', 11),
    -- Room 05 / E08: source column 12
    (UNHEX('d72dda347f2d542db02e1bdacd859995'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E08', 'E', 'STANDARD', 12),
    -- Room 05 / E09: source column 13
    (UNHEX('7e9b17797df85df0ac1b29a5cd467569'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E09', 'E', 'STANDARD', 13),
    -- Room 05 / E10: source column 14
    (UNHEX('a870f7cd29e95bc7bb99713c9ff887b1'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E10', 'E', 'STANDARD', 14),
    -- Room 05 / E11: source column 15
    (UNHEX('4f13f8d5a4e75d89ac92e8b039fc000d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E11', 'E', 'STANDARD', 15),
    -- Room 05 / E12: source column 16
    (UNHEX('df6fa03dd54151f98ee7eeb4c5ac168f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E12', 'E', 'STANDARD', 16),
    -- Room 05 / E13: source column 17
    (UNHEX('923f4b0f8cde5440bad9aba73e4a7475'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E13', 'E', 'STANDARD', 17),
    -- Room 05 / E14: source column 18
    (UNHEX('e1640d6b5c79516db26a6aa2789408b6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E14', 'E', 'STANDARD', 18),
    -- Room 05 / E15: source column 19
    (UNHEX('dff5d6fcfe105148b25545efba7f5fcb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E15', 'E', 'STANDARD', 19),
    -- Room 05 / F01: source column 5
    (UNHEX('ba0dc26d944251c7a988a878500b5ec6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F01', 'F', 'STANDARD', 5),
    -- Room 05 / F02: source column 6
    (UNHEX('8bdcbebaddef517cb110c73f80719877'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F02', 'F', 'STANDARD', 6),
    -- Room 05 / F03: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('c5107eee860f588b918d281c587cb57b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F03', 'F', 'STANDARD', 7),
    -- Room 05 / F04: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('18efe73956f95c58a37434b85f5f089c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F04', 'F', 'STANDARD', 8),
    -- Room 05 / F05: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('75313ba047b25aadaffb5f72ee9688e2'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F05', 'F', 'STANDARD', 9),
    -- Room 05 / F06: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('ed8bcd49b743509f9cdb46b6149a9813'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F06', 'F', 'STANDARD', 10),
    -- Room 05 / F07: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('f72e5968932d5a21b65eb6c3816d49a8'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F07', 'F', 'STANDARD', 11),
    -- Room 05 / F08: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('99bd58725a6d5c26903ff6fc71962559'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F08', 'F', 'STANDARD', 12),
    -- Room 05 / F09: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('9995899114a95cd6bdb01e9c43e46d97'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F09', 'F', 'STANDARD', 13),
    -- Room 05 / F10: source column 14
    (UNHEX('8b147c3198b454e38cb43073a3064a79'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F10', 'F', 'STANDARD', 14),
    -- Room 05 / F11: source column 15
    (UNHEX('962d6685bd295471a06090e6fc4fbe80'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F11', 'F', 'STANDARD', 15),
    -- Room 05 / F12: source column 16
    (UNHEX('6d1242b72dd6582ca8c9952d46d79aef'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F12', 'F', 'STANDARD', 16),
    -- Room 05 / F13: source column 17
    (UNHEX('b62a2ceec1155b89a7f0735462bd8363'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F13', 'F', 'STANDARD', 17),
    -- Room 05 / F14: source column 18
    (UNHEX('5e8efdae7bab5de292c8b285295f937c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F14', 'F', 'STANDARD', 18),
    -- Room 05 / F15: source column 19
    (UNHEX('c06f372125e15d5baee5db45419f397e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F15', 'F', 'STANDARD', 19),
    -- Room 05 / G01: source column 1
    (UNHEX('887e929075ea59e280e0335c79dac7ed'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G01', 'G', 'STANDARD', 1),
    -- Room 05 / G02: source column 2
    (UNHEX('1173a6178a445ff7b0b81953ff84e4ad'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G02', 'G', 'STANDARD', 2),
    -- Room 05 / G03: source column 5
    (UNHEX('22cb31aa89975fe99fc72ff378a8e0ee'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G03', 'G', 'STANDARD', 5),
    -- Room 05 / G04: source column 6
    (UNHEX('715231680e015c8886a7d5be1967efa3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G04', 'G', 'STANDARD', 6),
    -- Room 05 / G05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('7ab86be8438652089ce4d363f5e33b29'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G05', 'G', 'STANDARD', 7),
    -- Room 05 / G06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('eb9256472a585e6c80ec28cccf66022c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G06', 'G', 'STANDARD', 8),
    -- Room 05 / G07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('d2d92864d5bc54d8967e360cd3125499'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G07', 'G', 'STANDARD', 9),
    -- Room 05 / G08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('749a9107e78c51419fe2aa28174b70d8'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G08', 'G', 'STANDARD', 10),
    -- Room 05 / G09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('bf3015ffb3f45bb7af31474dc9298869'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G09', 'G', 'STANDARD', 11),
    -- Room 05 / G10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('4dabf44317945a85a13b7d1886e4e35a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G10', 'G', 'STANDARD', 12),
    -- Room 05 / G11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('301fa51322255d32a9328950a5e167ab'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G11', 'G', 'STANDARD', 13),
    -- Room 05 / G12: source column 14
    (UNHEX('dfbadd42d00f54f3bd9ecc466a84bc90'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G12', 'G', 'STANDARD', 14),
    -- Room 05 / G13: source column 15
    (UNHEX('073aeca6a9ab585eae16a0938abb61c4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G13', 'G', 'STANDARD', 15),
    -- Room 05 / G14: source column 16
    (UNHEX('aad17fd3e58151e0a421e8002de99bcb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G14', 'G', 'STANDARD', 16),
    -- Room 05 / G15: source column 17
    (UNHEX('0e99f5e16f4e5fb79ddc8d6c1af2371e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G15', 'G', 'STANDARD', 17),
    -- Room 05 / G16: source column 18
    (UNHEX('66187e466595554eabf60578e3ddc8cb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G16', 'G', 'STANDARD', 18),
    -- Room 05 / G17: source column 19
    (UNHEX('f212027e74185d34ac964f5ad2a766e1'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G17', 'G', 'STANDARD', 19),
    -- Room 05 / H01: source column 1
    (UNHEX('8f9a98ecdbfb5a5db446ae66c595fc5a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H01', 'H', 'STANDARD', 1),
    -- Room 05 / H02: source column 2
    (UNHEX('fabf90a52e305f0fb89f4660a6543475'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H02', 'H', 'STANDARD', 2),
    -- Room 05 / H03: source column 5
    (UNHEX('36ccbdf32c025e0cbd3f91bcd56cef06'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H03', 'H', 'STANDARD', 5),
    -- Room 05 / H04: source column 6
    (UNHEX('7e7bada8901a5119bc5256a023808d3f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H04', 'H', 'STANDARD', 6),
    -- Room 05 / H05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('2039954d67d75fac97188143cc77580a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H05', 'H', 'STANDARD', 7),
    -- Room 05 / H06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('1f7b2f42e6115583b9b70a67640f3466'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H06', 'H', 'STANDARD', 8),
    -- Room 05 / H07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('c96d605c2e8450c884736183a3479d88'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H07', 'H', 'STANDARD', 9),
    -- Room 05 / H08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('74646020548d5ed8ab84f9dd127cec33'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H08', 'H', 'STANDARD', 10),
    -- Room 05 / H09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('d793b6889377505e9396e43dbee5bcab'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H09', 'H', 'STANDARD', 11),
    -- Room 05 / H10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('63ec6940fc365d6995d7bae7675fd438'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H10', 'H', 'STANDARD', 12),
    -- Room 05 / H11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('3244fcf205dc551ba732ee611316f0bf'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H11', 'H', 'STANDARD', 13),
    -- Room 05 / H12: source column 14
    (UNHEX('d7c9a11e49f55ba29125d66c2eb191a8'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H12', 'H', 'STANDARD', 14),
    -- Room 05 / H13: source column 15
    (UNHEX('3ee66b57ae765e5a9fea43c69c8c7e06'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H13', 'H', 'STANDARD', 15),
    -- Room 05 / H14: source column 16
    (UNHEX('dc0f2e57e66f5ed2b5b49f682d505896'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H14', 'H', 'STANDARD', 16),
    -- Room 05 / H15: source column 17
    (UNHEX('1130738d751f58e1b0c5af5093a0da4a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H15', 'H', 'STANDARD', 17),
    -- Room 05 / H16: source column 18
    (UNHEX('39c14840204c5951a7e9994531419881'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H16', 'H', 'STANDARD', 18),
    -- Room 05 / H17: source column 19
    (UNHEX('d03e68c1d9035520806b236b494f437a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H17', 'H', 'STANDARD', 19),
    -- Room 05 / J01: source column 1
    (UNHEX('2168802251645f1bae8adb4e75f43c65'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J01', 'J', 'STANDARD', 1),
    -- Room 05 / J02: source column 2
    (UNHEX('a182b470a42057b0acfd41ec327b4c2f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J02', 'J', 'STANDARD', 2),
    -- Room 05 / J03: source column 5
    (UNHEX('9b5bc184da8b57519361494e3c9103fd'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J03', 'J', 'STANDARD', 5),
    -- Room 05 / J04: source column 6
    (UNHEX('3ebe575908c55d78ab70f9d6fe4e2448'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J04', 'J', 'STANDARD', 6),
    -- Room 05 / J05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('6ebd0aba5b8950daa98c208fd35da780'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J05', 'J', 'STANDARD', 7),
    -- Room 05 / J06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('a7d97537f9845bcb8183c2be5830ab3e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J06', 'J', 'STANDARD', 8),
    -- Room 05 / J07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('cc813a08f42e5a9eb6358521f7eca98c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J07', 'J', 'STANDARD', 9),
    -- Room 05 / J08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('5d41e779aae75741b3c0535dddb77aac'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J08', 'J', 'STANDARD', 10),
    -- Room 05 / J09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('37302d7486be5062aa4ab9648589ef4d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J09', 'J', 'STANDARD', 11),
    -- Room 05 / J10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('8e7d41d715bd519daaac65e98d5cea17'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J10', 'J', 'STANDARD', 12),
    -- Room 05 / J11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('9bed43ce0a795c48afd8e71e85644325'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J11', 'J', 'STANDARD', 13),
    -- Room 05 / J12: source column 14
    (UNHEX('bbe476f6e7d25b359a87f7b2b655ae79'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J12', 'J', 'STANDARD', 14),
    -- Room 05 / J13: source column 15
    (UNHEX('a3189a6bd25a506f8265188254196c40'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J13', 'J', 'STANDARD', 15),
    -- Room 05 / J14: source column 16
    (UNHEX('ab62e1e47e1b523d8ca3b31c8eab9991'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J14', 'J', 'STANDARD', 16),
    -- Room 05 / J15: source column 17
    (UNHEX('935371100489540f99fc4f80e5c6d8c3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J15', 'J', 'STANDARD', 17),
    -- Room 05 / J16: source column 18
    (UNHEX('39594717d7dd5f9bb28a25140fe9d750'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J16', 'J', 'STANDARD', 18),
    -- Room 05 / J17: source column 19
    (UNHEX('49fd02b3e3875580978c13926b65f15a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J17', 'J', 'STANDARD', 19),
    -- Room 05 / K01: source column 1
    (UNHEX('55c5e03c7923595cbdaf3e711339b6e2'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K01', 'K', 'STANDARD', 1),
    -- Room 05 / K02: source column 2
    (UNHEX('b84e41f814f95fca92dd60eeb2eb9f65'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K02', 'K', 'STANDARD', 2),
    -- Room 05 / K03: source column 5
    (UNHEX('b6c1990b1b445dd984af11208a6be15a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K03', 'K', 'STANDARD', 5),
    -- Room 05 / K04: source column 6
    (UNHEX('502bba151bae5cc2bdcbb7a9987bb555'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K04', 'K', 'STANDARD', 6),
    -- Room 05 / K05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('f90d0510b04553cb9a2633a44730ffde'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K05', 'K', 'STANDARD', 7),
    -- Room 05 / K06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('cd38804fe6155cb29aaba4df3204080f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K06', 'K', 'STANDARD', 8),
    -- Room 05 / K07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('53afe1a3efdd56b5883595201e1ed577'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K07', 'K', 'STANDARD', 9),
    -- Room 05 / K08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('c49e4daacc19552eae2757b48d2abe9d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K08', 'K', 'STANDARD', 10),
    -- Room 05 / K09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('8ff19d039a2853928693fd011e1ecdbe'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K09', 'K', 'STANDARD', 11),
    -- Room 05 / K10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('daad34bb05565b95ab0f9491d8d2d001'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K10', 'K', 'STANDARD', 12),
    -- Room 05 / K11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('979f7a0b0e4d5be1afab6a4cec5471de'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K11', 'K', 'STANDARD', 13),
    -- Room 05 / K12: source column 14
    (UNHEX('ab6de71dd6f359c8908633d9b52a046e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K12', 'K', 'STANDARD', 14),
    -- Room 05 / K13: source column 15
    (UNHEX('b42a04f14d7e55ee836845b52deadf43'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K13', 'K', 'STANDARD', 15),
    -- Room 05 / K14: source column 16
    (UNHEX('a688bd31238f5b1e851a73aa933eef5f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K14', 'K', 'STANDARD', 16),
    -- Room 05 / K15: source column 17
    (UNHEX('3ae2eabdc4ad5bf795697da46249a359'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K15', 'K', 'STANDARD', 17),
    -- Room 05 / K16: source column 18
    (UNHEX('c46cfe9e3c8955aebecc6d4136c8a54b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K16', 'K', 'STANDARD', 18),
    -- Room 05 / L01: source column 1
    (UNHEX('3353356834585784ab83b7adf5f9b34c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L01', 'L', 'STANDARD', 1),
    -- Room 05 / L02: source column 2
    (UNHEX('f759f40317d45412a325fc3304718eec'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L02', 'L', 'STANDARD', 2),
    -- Room 05 / L03: source column 5
    (UNHEX('3707cbdeb65854d9989fff9cde7b1bca'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L03', 'L', 'STANDARD', 5),
    -- Room 05 / L04: source column 6
    (UNHEX('737fef30543f58ccbd9bf34f1f876abf'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L04', 'L', 'STANDARD', 6),
    -- Room 05 / L05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('23bbcd6643bb519783d31c7625aea205'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L05', 'L', 'STANDARD', 7),
    -- Room 05 / L06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('401a16f9e0525569af98af45af857088'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L06', 'L', 'STANDARD', 8),
    -- Room 05 / L07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('dfd71f16d0c95a3998895b26dd5e9e59'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L07', 'L', 'STANDARD', 9),
    -- Room 05 / L08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('5e89fcc58ebe5a35b658e8d8799d4e5b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L08', 'L', 'STANDARD', 10),
    -- Room 05 / L09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('3b9b269377b15107b31894b51090b220'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L09', 'L', 'STANDARD', 11),
    -- Room 05 / L10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('cd5d221edd705fc4b23d2eccb5bc4f72'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L10', 'L', 'STANDARD', 12),
    -- Room 05 / L11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('9f16e21db58b52b097cb6a2b2a5c5f9f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L11', 'L', 'STANDARD', 13),
    -- Room 05 / L12: source column 14
    (UNHEX('597f39e3f4895571ae313a637f78b4fe'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L12', 'L', 'STANDARD', 14),
    -- Room 05 / L13: source column 15
    (UNHEX('fa3263eceb075c1c9a00376dbc49e855'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L13', 'L', 'STANDARD', 15),
    -- Room 05 / L14: source column 16
    (UNHEX('157d5147d45453fd8b193fa423c8252b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L14', 'L', 'STANDARD', 16),
    -- Room 05 / L15: source column 17
    (UNHEX('3c7bcccbf3e554459cd719a647206e31'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L15', 'L', 'STANDARD', 17),
    -- Room 05 / L16: source column 18
    (UNHEX('5850cc548de157599af026f05c7150fb'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L16', 'L', 'STANDARD', 18),
    -- Room 05 / L17: source column 19
    (UNHEX('80d64068af4f5574ac7c68459f2089da'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L17', 'L', 'STANDARD', 19),
    -- Room 05 / M01: source column 1
    (UNHEX('919b804368665f2d962781dfcf8f8d34'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M01', 'M', 'STANDARD', 1),
    -- Room 05 / M02: source column 2
    (UNHEX('2034bdfcf55d555485adfa04b5124100'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M02', 'M', 'STANDARD', 2),
    -- Room 05 / M03: source column 5
    (UNHEX('75173a68556a5530a0644ce21cf52af0'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M03', 'M', 'STANDARD', 5),
    -- Room 05 / M04: source column 6
    (UNHEX('ce4be115708b5a898dc2064a4f869848'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M04', 'M', 'STANDARD', 6),
    -- Room 05 / M05: source column 7; CSS seat-vip, visible single-seat legend
    (UNHEX('7f5e1aeff15b50ed93c23aeb245dac57'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M05', 'M', 'STANDARD', 7),
    -- Room 05 / M06: source column 8; CSS seat-vip, visible single-seat legend
    (UNHEX('c6275c461eec506ba426915e43f62ea6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M06', 'M', 'STANDARD', 8),
    -- Room 05 / M07: source column 9; CSS seat-vip, visible single-seat legend
    (UNHEX('372139dcf46c56b5a1ac0c43692cd79c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M07', 'M', 'STANDARD', 9),
    -- Room 05 / M08: source column 10; CSS seat-vip, visible single-seat legend
    (UNHEX('fd1c4a2010f65a51aa0118e3c6cdc973'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M08', 'M', 'STANDARD', 10),
    -- Room 05 / M09: source column 11; CSS seat-vip, visible single-seat legend
    (UNHEX('8586d92d67f15ca89e16f19190a58a7e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M09', 'M', 'STANDARD', 11),
    -- Room 05 / M10: source column 12; CSS seat-vip, visible single-seat legend
    (UNHEX('0bb0cda609765eb68581d87524346b83'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M10', 'M', 'STANDARD', 12),
    -- Room 05 / M11: source column 13; CSS seat-vip, visible single-seat legend
    (UNHEX('cd41c101140056d1a1b8ea0932e0b54a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M11', 'M', 'STANDARD', 13),
    -- Room 05 / M12: source column 14
    (UNHEX('321036eca2615d958974c8fb40020538'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M12', 'M', 'STANDARD', 14),
    -- Room 05 / M13: source column 15
    (UNHEX('e8c7bd19054b5f3396929f1f439332f4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M13', 'M', 'STANDARD', 15),
    -- Room 05 / M14: source column 16
    (UNHEX('a02964f98a355badaf380600a00cbfcc'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M14', 'M', 'STANDARD', 16),
    -- Room 05 / M15: source column 17
    (UNHEX('fa652c94c284527fb260be74e8fc0118'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M15', 'M', 'STANDARD', 17),
    -- Room 05 / M16: source column 18
    (UNHEX('08d778f97eb3507eac8fe71044e7b682'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M16', 'M', 'STANDARD', 18),
    -- Room 05 / N01: source column 1
    (UNHEX('b558a4311fd55382a79e1bcf4c7fbcc3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N01', 'N', 'STANDARD', 1),
    -- Room 05 / N02: source column 2
    (UNHEX('acbdae4cb59255b98a76194042b669a2'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N02', 'N', 'STANDARD', 2),
    -- Room 05 / N03: source column 5
    (UNHEX('0d3a86a6521d5b7ea5fd4c787442b40b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N03', 'N', 'STANDARD', 5),
    -- Room 05 / N04: source column 6
    (UNHEX('3f20099c4b00517591b938f0f5ff568f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N04', 'N', 'STANDARD', 6),
    -- Room 05 / N05: source column 7
    (UNHEX('8d967d9c5f405579920773beb1ee88bf'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N05', 'N', 'STANDARD', 7),
    -- Room 05 / N06: source column 8
    (UNHEX('dca0cd719d0b53719c0d9ff04ae1f761'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N06', 'N', 'STANDARD', 8),
    -- Room 05 / N07: source column 9
    (UNHEX('89d6d07561e755ad980e5017fa2938f6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N07', 'N', 'STANDARD', 9),
    -- Room 05 / N08: source column 10
    (UNHEX('4496d78642185f049b7bd298c33901c6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N08', 'N', 'STANDARD', 10),
    -- Room 05 / N09: source column 11
    (UNHEX('36bb5438a689529faba24aec84d4e251'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N09', 'N', 'STANDARD', 11),
    -- Room 05 / N10: source column 12
    (UNHEX('ddfb95be1e135e81bb4a5745117352fc'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N10', 'N', 'STANDARD', 12),
    -- Room 05 / N11: source column 13
    (UNHEX('901a431779235861bb07fb26a905da2c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N11', 'N', 'STANDARD', 13),
    -- Room 05 / N12: source column 14
    (UNHEX('66d3d70c67e15e358933b690f521db8c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N12', 'N', 'STANDARD', 14),
    -- Room 05 / N13: source column 15
    (UNHEX('53a4213a14f55d6db9261f4dc4c0d7af'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N13', 'N', 'STANDARD', 15),
    -- Room 05 / N14: source column 16
    (UNHEX('b4102d0501a858e4a90a6bcb5d76d619'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N14', 'N', 'STANDARD', 16),
    -- Room 05 / N15: source column 17
    (UNHEX('be2e4554fac05411a385718cae64c713'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N15', 'N', 'STANDARD', 17),
    -- Room 05 / N16: source column 18
    (UNHEX('e416529725b952ee82a9580235915302'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N16', 'N', 'STANDARD', 18),
    -- Room 05 / N17: source column 19
    (UNHEX('d72cb5338e9452b391e822843b225bfc'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N17', 'N', 'STANDARD', 19),
    -- Room 05 / O01: source column 1
    (UNHEX('e9593c52880c5af1aee1d0126ecce16b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O01', 'O', 'STANDARD', 1),
    -- Room 05 / O02: source column 2
    (UNHEX('f5d0d45d37e755efa723a4dc3964bca9'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O02', 'O', 'STANDARD', 2),
    -- Room 05 / O03: source column 5
    (UNHEX('2769c1475ce25d04b833b88a931faf85'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O03', 'O', 'STANDARD', 5),
    -- Room 05 / O04: source column 6
    (UNHEX('b33c16daebbe50a8b70b463e169ebfc0'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O04', 'O', 'STANDARD', 6),
    -- Room 05 / O05: source column 7
    (UNHEX('f44e5d3f006558b393c553c5f43b3b57'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O05', 'O', 'STANDARD', 7),
    -- Room 05 / O06: source column 8
    (UNHEX('90751c7394b85b45862f078586361aa6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O06', 'O', 'STANDARD', 8),
    -- Room 05 / O07: source column 9
    (UNHEX('3949b059a58e59cf922d5f0c69a5b078'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O07', 'O', 'STANDARD', 9),
    -- Room 05 / O08: source column 10
    (UNHEX('f336c602b8095f17b02d7f590d9b2712'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O08', 'O', 'STANDARD', 10),
    -- Room 05 / O09: source column 11
    (UNHEX('e8ba9c8774ca574980e3eb98385b9ef6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O09', 'O', 'STANDARD', 11),
    -- Room 05 / O10: source column 12
    (UNHEX('6ebdd592804d5e2b9f71c59342fae1d7'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O10', 'O', 'STANDARD', 12),
    -- Room 05 / O11: source column 13
    (UNHEX('cbbf6134134f5d1ba7d93b2413f9db40'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O11', 'O', 'STANDARD', 13),
    -- Room 05 / O12: source column 14
    (UNHEX('182f0ed3fb3a5917bbb614a5dcdf3850'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O12', 'O', 'STANDARD', 14),
    -- Room 05 / O13: source column 15
    (UNHEX('2de2df44ebee5e79aa8aff78d79c8c75'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O13', 'O', 'STANDARD', 15),
    -- Room 05 / O14: source column 16
    (UNHEX('faeca5b2ddbc541197963e78de6bf4d3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O14', 'O', 'STANDARD', 16),
    -- Room 05 / O15: source column 17
    (UNHEX('de01cacb616357f1a77206249d58caf5'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O15', 'O', 'STANDARD', 17),
    -- Room 05 / O16: source column 18
    (UNHEX('2b886c5edb8e58e38fbae8a197ac0bf0'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O16', 'O', 'STANDARD', 18),
    -- Room 05 / P01: source column 1
    (UNHEX('fd8db0fb10ca5c9fac7f7e2caa084c15'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P01', 'P', 'STANDARD', 1),
    -- Room 05 / P02: source column 2
    (UNHEX('879bb75035ad52f2a1f987714f15e668'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P02', 'P', 'STANDARD', 2),
    -- Room 05 / P03: source column 5
    (UNHEX('eb62fe7754ea5560a4798b7bbd0142ef'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P03', 'P', 'STANDARD', 5),
    -- Room 05 / P04: source column 6
    (UNHEX('4d244c61647452e2a32553fb6e27f3ef'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P04', 'P', 'STANDARD', 6),
    -- Room 05 / P05: source column 7
    (UNHEX('14072714193259148c8b47f7c05ae7e0'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P05', 'P', 'STANDARD', 7),
    -- Room 05 / P06: source column 8
    (UNHEX('8843ca881b8057e6bbb2616beb8468ad'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P06', 'P', 'STANDARD', 8),
    -- Room 05 / P07: source column 9
    (UNHEX('9a545d7fd4905120a3e04f4fe7b4c016'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P07', 'P', 'STANDARD', 9),
    -- Room 05 / P08: source column 10
    (UNHEX('820bfcf7818754fbb4776dc3d1496d3a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P08', 'P', 'STANDARD', 10),
    -- Room 05 / P09: source column 11
    (UNHEX('92790852d51553d9befac2bef754092e'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P09', 'P', 'STANDARD', 11),
    -- Room 05 / P10: source column 12
    (UNHEX('3982d2f2372859dbb0f64d0ed96ad83b'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P10', 'P', 'STANDARD', 12),
    -- Room 05 / P11: source column 13
    (UNHEX('4e2a0c3d35165d42af226e82852833d6'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P11', 'P', 'STANDARD', 13),
    -- Room 05 / P12: source column 14
    (UNHEX('9e9f4435ce165f5e89d98e1546cef9d5'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P12', 'P', 'STANDARD', 14),
    -- Room 05 / P13: source column 15
    (UNHEX('c04ec9e1a86b5c8882e5952e934ef77f'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P13', 'P', 'STANDARD', 15),
    -- Room 05 / P14: source column 16
    (UNHEX('7f242b223f17532690a75dbca7c09051'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P14', 'P', 'STANDARD', 16),
    -- Room 05 / P15: source column 17
    (UNHEX('849cdf783aa4507393c85b43359ad27c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P15', 'P', 'STANDARD', 17),
    -- Room 05 / P16: source column 18
    (UNHEX('a9b86ec9395450b6abc4e709a6f108dc'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P16', 'P', 'STANDARD', 18),
    -- Room 05 / P17: source column 19
    (UNHEX('a3fbf55d85e2512fa1da656b46f1581c'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P17', 'P', 'STANDARD', 19),
    -- Room 05 / Q01: source column 1
    (UNHEX('879c7fede1715f51a5da489b9068687a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q01', 'Q', 'STANDARD', 1),
    -- Room 05 / Q02: source column 2
    (UNHEX('19db491fda9d5530a16b765ebaf95cba'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q02', 'Q', 'STANDARD', 2),
    -- Room 05 / Q03: source column 5
    (UNHEX('6cb7121d11da597e97bff414ce5f1f06'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q03', 'Q', 'STANDARD', 5),
    -- Room 05 / Q04: source column 6
    (UNHEX('d043affba9e85ea581d6722f6d96e988'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q04', 'Q', 'STANDARD', 6),
    -- Room 05 / Q05: source column 7
    (UNHEX('eb957a44e2e159e8a0a541402ba94365'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q05', 'Q', 'STANDARD', 7),
    -- Room 05 / Q06: source column 8
    (UNHEX('2b3efecd1c8156788daa92ee18971137'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q06', 'Q', 'STANDARD', 8),
    -- Room 05 / Q07: source column 9
    (UNHEX('19adc3598baa51a189ddd0983f11d2f7'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q07', 'Q', 'STANDARD', 9),
    -- Room 05 / Q08: source column 13
    (UNHEX('238b880166c55e3bbada7f7269fc1c56'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q08', 'Q', 'STANDARD', 13),
    -- Room 05 / Q09: source column 14
    (UNHEX('26962d1d2659559789aeec88067b5f10'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q09', 'Q', 'STANDARD', 14),
    -- Room 05 / Q10: source column 15
    (UNHEX('d9dd3f2589505a019363038ff135524a'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q10', 'Q', 'STANDARD', 15),
    -- Room 05 / Q11: source column 16
    (UNHEX('dc1ab33b234253f8bff2d29f237e6b57'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q11', 'Q', 'STANDARD', 16),
    -- Room 05 / Q12: source column 17
    (UNHEX('81e9f2f007395c439c2e56e369c573d4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q12', 'Q', 'STANDARD', 17),
    -- Room 05 / Q13: source column 18
    (UNHEX('068e86c7496b5ee0bcd8dc0113dacab5'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q13', 'Q', 'STANDARD', 18),
    -- Room 05 / Q14: source column 19
    (UNHEX('0f45cf971c915e8c8b8ed02c60e27bc4'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q14', 'Q', 'STANDARD', 19),
    -- Room 05 / R01: source column 1
    (UNHEX('990ff46cfbca5c288d8f0202d2ce3d87'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R01', 'R', 'COUPLE', 1),
    -- Room 05 / R02: source column 2
    (UNHEX('5ecb6627efd358fcb444168a77ecac62'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R02', 'R', 'COUPLE', 2),
    -- Room 05 / R03: source column 3
    (UNHEX('908ae43f991b5daea4c7fda4491c37d5'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R03', 'R', 'COUPLE', 3),
    -- Room 05 / R04: source column 11
    (UNHEX('a9b8c7e393e0594b8ce4412bda7ff91d'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R04', 'R', 'COUPLE', 11),
    -- Room 05 / R05: source column 12
    (UNHEX('3987a14945a65d1fb4a384cf885a0b00'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R05', 'R', 'COUPLE', 12),
    -- Room 05 / R06: source column 13
    (UNHEX('b881095970af58b882a70811aec29ab3'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R06', 'R', 'COUPLE', 13);

INSERT INTO cinestar_qt_prices (id, showtime_id, room_id, seat_number, price, snapshot_status) VALUES
    (UNHEX('185eba45e23d5eefa0435f91eced750c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A01', 79000, 'AVAILABLE'),
    (UNHEX('009f81f456b952ebb0696262194ff655'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A02', 79000, 'AVAILABLE'),
    (UNHEX('ce2fc8033d5b5e6789569f032550dbbc'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A03', 79000, 'AVAILABLE'),
    (UNHEX('a468fff230ca5362ad9e69d7ed422db5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A04', 79000, 'AVAILABLE'),
    (UNHEX('7e8c065f3e9b5d7cb963d4fef46c45ce'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A05', 79000, 'AVAILABLE'),
    (UNHEX('8dc674c0c77b53d09a57df0d19d5d1e0'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A06', 79000, 'UNAVAILABLE'),
    (UNHEX('98cd19d1c50b5f9cbc73522782e3492e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A07', 79000, 'UNAVAILABLE'),
    (UNHEX('59285aee06a15cd9952553348f720477'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A08', 79000, 'AVAILABLE'),
    (UNHEX('2256e08c3e2e5a13b3f6c514773111a8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A09', 79000, 'AVAILABLE'),
    (UNHEX('9e8540a8241754a4955a031968251130'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A10', 79000, 'AVAILABLE'),
    (UNHEX('304e4ee2328756088f35991e0048ca00'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A11', 79000, 'AVAILABLE'),
    (UNHEX('a86cd350e552521382c4ef92b4dcce82'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A12', 79000, 'AVAILABLE'),
    (UNHEX('0823dd2fa79853239c4e103f8fedc3e7'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A13', 79000, 'AVAILABLE'),
    (UNHEX('c892b1bbf4825a3cbe9e180ae9134a29'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'A14', 79000, 'AVAILABLE'),
    (UNHEX('af4f06869a2f514a94dac569976f450e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B01', 79000, 'AVAILABLE'),
    (UNHEX('38bcc9ab0a2e51a0b67d082b02095617'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B02', 79000, 'AVAILABLE'),
    (UNHEX('4631297515015344a2ddd06a893d436b'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B03', 79000, 'AVAILABLE'),
    (UNHEX('0c85878960315b119f1b0ce3a7cba9bd'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B04', 79000, 'AVAILABLE'),
    (UNHEX('200a9ac5ccc358cfa6637dca9494ec7b'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B05', 79000, 'AVAILABLE'),
    (UNHEX('7f923fd21ff959df90fa6111d4952bcb'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B06', 79000, 'AVAILABLE'),
    (UNHEX('2d349064509258a49d1f9e31741a765e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B07', 79000, 'AVAILABLE'),
    (UNHEX('77fa1e08bf175df590d228ad38371962'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B08', 79000, 'AVAILABLE'),
    (UNHEX('349d95c168885126a3af2c0fcde172f2'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B09', 79000, 'AVAILABLE'),
    (UNHEX('d0a0546bf8ed50b1a2de84df092c5f80'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B10', 79000, 'AVAILABLE'),
    (UNHEX('685efa5d95615253acb7dd20c0eb0144'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B11', 79000, 'AVAILABLE'),
    (UNHEX('f073613e15eb545eb89b74d3116cd290'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B12', 79000, 'AVAILABLE'),
    (UNHEX('fcdc2c458ab15186b7cb9953f9b81a5f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B13', 79000, 'AVAILABLE'),
    (UNHEX('587bc537eda25c33988562d9777c6cfa'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'B14', 79000, 'AVAILABLE'),
    (UNHEX('06ce482b53f55677ab8f33d8ec59e1ee'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C01', 79000, 'AVAILABLE'),
    (UNHEX('a4e9c0e9ad795c14aa82daa9b8a7d0b1'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C02', 79000, 'AVAILABLE'),
    (UNHEX('e4628b60a32d592581b031be1d0912f3'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C03', 79000, 'AVAILABLE'),
    (UNHEX('019b7d0dcd1055268271b95999c4a021'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C04', 79000, 'AVAILABLE'),
    (UNHEX('5a2c442ee465546e9e179e4c4d4428ec'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C05', 79000, 'AVAILABLE'),
    (UNHEX('4084514a842351fd9347ef5879e138d8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C06', 79000, 'AVAILABLE'),
    (UNHEX('d08bcfd4171a5e999ab03f5704ac1563'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C07', 79000, 'AVAILABLE'),
    (UNHEX('7a704414a8ae58e4bb64e802a7436fb8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C08', 79000, 'AVAILABLE'),
    (UNHEX('588f92120a7a5545a8dbd711a60ea02a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C09', 79000, 'AVAILABLE'),
    (UNHEX('59db11b91b4252f18312f38b4c7f3281'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C10', 79000, 'AVAILABLE'),
    (UNHEX('5819a35701605cd2a6d924c268c41b49'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C11', 79000, 'AVAILABLE'),
    (UNHEX('0f183dfdbffb505f864d16addb769af8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C12', 79000, 'AVAILABLE'),
    (UNHEX('02a6d44c191f546d9db61d735a89d276'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C13', 79000, 'AVAILABLE'),
    (UNHEX('2114f95ad3b05d2a9191c9c89db75762'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C14', 79000, 'AVAILABLE'),
    (UNHEX('31b8b8661b27533c91ca329c95ef3073'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C15', 79000, 'AVAILABLE'),
    (UNHEX('6a45db5478835d7e80f6c483908cdbd1'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C16', 79000, 'AVAILABLE'),
    (UNHEX('0ddd52f2a93f50da871f3da1baa937c6'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'C17', 79000, 'AVAILABLE'),
    (UNHEX('9d85ecfdd69a536cb4a8a19db99c3d30'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D01', 79000, 'AVAILABLE'),
    (UNHEX('fd6d365d3a625befb10a41b5f0b684b0'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D02', 79000, 'AVAILABLE'),
    (UNHEX('db1fa6f5dac15532a0e80c60364a10d6'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D03', 79000, 'AVAILABLE'),
    (UNHEX('a89dda8132a15ceb90c62846321e0bc9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D04', 79000, 'AVAILABLE'),
    (UNHEX('977b54b606d95445a5e9f171ecba62d4'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D05', 79000, 'AVAILABLE'),
    (UNHEX('2d0e20926fd8583ab5a168cd412035ef'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D06', 79000, 'AVAILABLE'),
    (UNHEX('ef22bdac1d4a5c109b06c553f8e4d9d2'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D07', 79000, 'AVAILABLE'),
    (UNHEX('da5d3796aa6a5a78b26cd94f6393aa80'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D08', 79000, 'AVAILABLE'),
    (UNHEX('0064b7c228885db098a9361e6410979d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D09', 79000, 'AVAILABLE'),
    (UNHEX('7c1b84bb1abb5c7fba9aaa8db1d30b6a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D10', 79000, 'AVAILABLE'),
    (UNHEX('012cf888a66a573eac6f6c8ec1ab5cce'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D11', 79000, 'AVAILABLE'),
    (UNHEX('9e9af4064b35556dbfe48a2f671746a7'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D12', 79000, 'AVAILABLE'),
    (UNHEX('0f4057cd070c5af9b64c7db3140bc6da'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D13', 79000, 'AVAILABLE'),
    (UNHEX('f047e255afe65da6aaa906d1be5eca4d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D14', 79000, 'AVAILABLE'),
    (UNHEX('50bb999e9a9d5c079920455e14f200dd'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D15', 79000, 'AVAILABLE'),
    (UNHEX('48f7adaf8ae351e4a576b4aed172c8d5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D16', 79000, 'AVAILABLE'),
    (UNHEX('fa72f329b5c8507c87d728d31efa70ef'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'D17', 79000, 'AVAILABLE'),
    (UNHEX('1f8f33aed6d6570babbd0df9e9e85ebf'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E01', 79000, 'AVAILABLE'),
    (UNHEX('9f83192c63ec5c879e423e8ada10cd1c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E02', 79000, 'AVAILABLE'),
    (UNHEX('0b986a4febac5cac8b92b7e27d20bcba'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E03', 79000, 'AVAILABLE'),
    (UNHEX('732b14ac1059514fb7867c902a4537b6'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E04', 79000, 'AVAILABLE'),
    (UNHEX('3a71747040265c9aa7930deef1b33d9d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E05', 79000, 'AVAILABLE'),
    (UNHEX('d6dbf34df56e5a61acaa66d30651e594'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E06', 79000, 'AVAILABLE'),
    (UNHEX('5c1a05203c9c5f008e1c9d7a800c6746'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E07', 79000, 'AVAILABLE'),
    (UNHEX('c178d4c03e675699bf98267272a88fcf'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E08', 79000, 'AVAILABLE'),
    (UNHEX('a6e5b055bf955655809879df541f2f7f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E09', 79000, 'AVAILABLE'),
    (UNHEX('470576f069405f2fa08499b6cd5d8dc9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E10', 79000, 'AVAILABLE'),
    (UNHEX('04d33be92e3b57bd9f4636e196b7cf0a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E11', 79000, 'AVAILABLE'),
    (UNHEX('436b9819e9f25ea0ae9874d847ca4b1e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E12', 79000, 'AVAILABLE'),
    (UNHEX('88cfb659f8f75b2d8183bc5d09f0caf8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E13', 79000, 'AVAILABLE'),
    (UNHEX('ada933580679518ebb784d1dc9d4435e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E14', 79000, 'AVAILABLE'),
    (UNHEX('cf53d9aaf0cf59788a1fe3af2613c7e8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E15', 79000, 'AVAILABLE'),
    (UNHEX('7a5d48396e9e5990b369a0f8ee415657'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E16', 79000, 'AVAILABLE'),
    (UNHEX('409ecc3ec12e5958bad19a6f39b823e2'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'E17', 79000, 'AVAILABLE'),
    (UNHEX('7818d10ac5d05e318b17ee38c61e8c12'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F01', 79000, 'AVAILABLE'),
    (UNHEX('cab037c85b1156e49abe5ef8cfe5be5e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F02', 79000, 'AVAILABLE'),
    (UNHEX('b46e85dad68659a3816a7f86d8837d68'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F03', 79000, 'AVAILABLE'),
    (UNHEX('7e1116b24329530f9995c30d730ae85c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F04', 79000, 'AVAILABLE'),
    (UNHEX('b735d27531b55460a62ca62dd6187eb5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F05', 79000, 'AVAILABLE'),
    (UNHEX('c8db9a0ccaef5f6fbcf8c20cec22b6de'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F06', 79000, 'AVAILABLE'),
    (UNHEX('c432cd58c02954ff9edb529e218d006a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F07', 79000, 'AVAILABLE'),
    (UNHEX('a9a49fa4ba8b5d8fb4d8fcfa1f2002ef'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F08', 79000, 'AVAILABLE'),
    (UNHEX('2b500274ce5250eb9f6fd4ac7f5048f5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F09', 79000, 'AVAILABLE'),
    (UNHEX('12814955e8225e2696b2dbb9aabb4859'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F10', 79000, 'AVAILABLE'),
    (UNHEX('02832f78bccc59538a6fdba2b5cfd77a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F11', 79000, 'AVAILABLE'),
    (UNHEX('e4844a9f70c55d7e8f650796dbbc50d9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F12', 79000, 'AVAILABLE'),
    (UNHEX('05ae6a9873795430be1b89be31427a6e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F13', 79000, 'AVAILABLE'),
    (UNHEX('60760ab6b01c50dc9bdaa0a50c243f7b'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F14', 79000, 'AVAILABLE'),
    (UNHEX('1a4cad787f1a55ff9b2432ede4186666'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F15', 79000, 'AVAILABLE'),
    (UNHEX('b711b05a08a1596d811d26d7b1551a23'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F16', 79000, 'AVAILABLE'),
    (UNHEX('a76261a31d34526caad3b19f353acb08'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'F17', 79000, 'AVAILABLE'),
    (UNHEX('81267bfa7ced5a189e0860df9f123416'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G01', 79000, 'AVAILABLE'),
    (UNHEX('81f27819b52358729cecc36eaee38273'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G02', 79000, 'AVAILABLE'),
    (UNHEX('af7cf7adf5a05be89b339433eedd5434'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G03', 79000, 'AVAILABLE'),
    (UNHEX('9717fa63f53e55ba978025f68bd67577'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G04', 79000, 'AVAILABLE'),
    (UNHEX('fd4060581dcf578f9933429cd15ebca2'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G05', 79000, 'AVAILABLE'),
    (UNHEX('8aedb2525be055f880c8ef60340597e4'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G06', 79000, 'AVAILABLE'),
    (UNHEX('49e1f8117bd459aab9eececccdc51f00'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G07', 79000, 'AVAILABLE'),
    (UNHEX('ec8c607c4f3a51cb92d906e1b875287e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G08', 79000, 'AVAILABLE'),
    (UNHEX('b540c5a2856e5383936fcadaa413566c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G09', 79000, 'UNAVAILABLE'),
    (UNHEX('99cfb48d408353ec9febf50f244330f2'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G10', 79000, 'UNAVAILABLE'),
    (UNHEX('dc462602b9fa50b0a9a430a0e3727543'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G11', 79000, 'AVAILABLE'),
    (UNHEX('2969f1c36fcb569b9aa725b7c84fb687'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G12', 79000, 'AVAILABLE'),
    (UNHEX('9bedf270825c5bb4b94b6225f4f56f8c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G13', 79000, 'AVAILABLE'),
    (UNHEX('2ee4cc9e5bd85ad5984f21db2bff23a1'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G14', 79000, 'AVAILABLE'),
    (UNHEX('610cb4bcbeca5f78a85ea75a730aabc0'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G15', 79000, 'AVAILABLE'),
    (UNHEX('e38777ec6922586f909035c212055fcc'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G16', 79000, 'AVAILABLE'),
    (UNHEX('7e2dddcb157b5ae2b668906a9fb70116'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'G17', 79000, 'AVAILABLE'),
    (UNHEX('9703ca26b24f5a9d99e857a71c4b4b57'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H01', 79000, 'AVAILABLE'),
    (UNHEX('12964d16bfb3557f8e488f944d43472f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H02', 79000, 'AVAILABLE'),
    (UNHEX('03c66908e67c513aa156288bdb11dd56'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H03', 79000, 'AVAILABLE'),
    (UNHEX('394217ac11e252068636efdf36c08e46'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H04', 79000, 'AVAILABLE'),
    (UNHEX('339a21aac2755e88be8a6a3ee0ecbebd'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H05', 79000, 'AVAILABLE'),
    (UNHEX('f4260f6fe9155a659cb37a08fa1c8747'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H06', 79000, 'AVAILABLE'),
    (UNHEX('a2a258f1d013538494c0eda4423136dd'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H07', 79000, 'AVAILABLE'),
    (UNHEX('bd9fe7cbfb765614bb8c0c8ce79972a6'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H08', 79000, 'AVAILABLE'),
    (UNHEX('0bd283945989508db4d4ca185048999e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H09', 79000, 'AVAILABLE'),
    (UNHEX('8475d71c45b658b08b99327d4fb9b61a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H10', 79000, 'AVAILABLE'),
    (UNHEX('3ff665f1320551858eb910ad7ca23bf4'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H11', 79000, 'AVAILABLE'),
    (UNHEX('a512f51fc3015d8cb0339a88da853c8e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H12', 79000, 'AVAILABLE'),
    (UNHEX('7bf6c17cb5a15f57b65be7369da99a34'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H13', 79000, 'AVAILABLE'),
    (UNHEX('19aef809016452ccb27846c5bd09761a'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H14', 79000, 'AVAILABLE'),
    (UNHEX('c349760372b256998c6e59a1e09ee980'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H15', 79000, 'AVAILABLE'),
    (UNHEX('abafed88d4de5dfa98254f343d9c788f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H16', 79000, 'AVAILABLE'),
    (UNHEX('eeeb5cf362245d85b5d3f744c785302d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'H17', 79000, 'AVAILABLE'),
    (UNHEX('3e95171c72e0576fb285a03b2c3e5bcc'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J01', 79000, 'AVAILABLE'),
    (UNHEX('1615e06cc9b45d92ad3d21e4015367e4'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J02', 79000, 'AVAILABLE'),
    (UNHEX('f9d81fe65b875e1f818a9584c92f0fad'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J03', 79000, 'AVAILABLE'),
    (UNHEX('ee1096c10957549689326fddb772ceda'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J04', 79000, 'AVAILABLE'),
    (UNHEX('e7be6a762b895b2b8462a2d49c8b33c0'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J05', 79000, 'AVAILABLE'),
    (UNHEX('dde36d7a6c1256b0ab92795afa4053a9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J06', 79000, 'AVAILABLE'),
    (UNHEX('b668851f50615528b4c33b7463f5e8b6'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J07', 79000, 'AVAILABLE'),
    (UNHEX('8617d0d05bc15aeca9cc45178640680c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J08', 79000, 'AVAILABLE'),
    (UNHEX('03aac04c53ce59398b86dd034c8cdc1e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J09', 79000, 'AVAILABLE'),
    (UNHEX('e7adc331f14e56a4bbbaff53844bc688'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J10', 79000, 'AVAILABLE'),
    (UNHEX('95430b9ebc8858499582fc8700072ebf'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J11', 79000, 'AVAILABLE'),
    (UNHEX('25a7d23c6dc9552e9c596e56fedbda9f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J12', 79000, 'AVAILABLE'),
    (UNHEX('ff321ba8e9545908aa5eff890d403ffb'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J13', 79000, 'AVAILABLE'),
    (UNHEX('adfb78014d5159b7b8c835a4bb1c624f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J14', 79000, 'AVAILABLE'),
    (UNHEX('01cbad8a3be954e183b2b7b7710ba887'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J15', 79000, 'AVAILABLE'),
    (UNHEX('6a1cb4cde4265cdab2b08f3bf2d5fc9e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J16', 79000, 'AVAILABLE'),
    (UNHEX('8b3c559a4d4057bdae7a3725652a3208'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'J17', 79000, 'AVAILABLE'),
    (UNHEX('2b3b5ecafa2a55f7a160a4774061fea9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K01', 79000, 'AVAILABLE'),
    (UNHEX('efcb834273625bd082bdba66cf83a39c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K02', 79000, 'AVAILABLE'),
    (UNHEX('7040889c9cf75652af77959ebd725a2c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K03', 79000, 'AVAILABLE'),
    (UNHEX('92c2f1914d785f8b85a54e7b7d834c98'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K04', 79000, 'AVAILABLE'),
    (UNHEX('d5686b72ec1553208b654d61207f16ec'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K05', 79000, 'AVAILABLE'),
    (UNHEX('0e872fc324c7575c8f14bfaf27b5f0d0'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K06', 79000, 'AVAILABLE'),
    (UNHEX('5df8ce4b3ae15db097f3d7e105f5b1c8'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K07', 79000, 'AVAILABLE'),
    (UNHEX('048c400c59455a52b6a8d2515f407aeb'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K08', 79000, 'AVAILABLE'),
    (UNHEX('409e9f21581d56148fecf5a32db5a717'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K09', 79000, 'AVAILABLE'),
    (UNHEX('8e73219111e157eba300245ef34fc9cc'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K10', 79000, 'AVAILABLE'),
    (UNHEX('e345aec45e935338a4632f92767751e3'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K11', 79000, 'AVAILABLE'),
    (UNHEX('3bebfcf840b35801adb6d219548801c5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K12', 79000, 'AVAILABLE'),
    (UNHEX('306e9c977e7e523e9d7cfa6d1d35e178'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K13', 79000, 'AVAILABLE'),
    (UNHEX('895f5570141d5bb584de455ce3a327c4'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K14', 79000, 'AVAILABLE'),
    (UNHEX('4defacb45db85aa992413f517e2d8151'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K15', 79000, 'AVAILABLE'),
    (UNHEX('72ca0c48a6fb52ce8f4aa9798e317a94'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K16', 79000, 'AVAILABLE'),
    (UNHEX('0bdbd4f9b17959be8b39799e3db1088e'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'K17', 79000, 'AVAILABLE'),
    (UNHEX('8d738ed47ca25271a205e8394652c65d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L01', 79000, 'AVAILABLE'),
    (UNHEX('d74dce0adfe35aac92218e2c4e0931ed'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L02', 79000, 'AVAILABLE'),
    (UNHEX('07362b9d0fa65be4984d23e311db2443'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L03', 79000, 'AVAILABLE'),
    (UNHEX('8c578bb8478752b8965245483b6db5dd'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L04', 79000, 'AVAILABLE'),
    (UNHEX('e99ce612b14f5f70bb6a46122d6b33fe'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L05', 79000, 'AVAILABLE'),
    (UNHEX('1f47e986f127517ca53826071b200c1c'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L06', 79000, 'AVAILABLE'),
    (UNHEX('844bbe98c39457edbf1e52086a8cd647'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L07', 79000, 'AVAILABLE'),
    (UNHEX('5a73c6813e445c25988855039e13dc25'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L08', 79000, 'AVAILABLE'),
    (UNHEX('c2d13ded774855ffa0d90a2ce86927c9'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L09', 79000, 'AVAILABLE'),
    (UNHEX('0b9e4fca2bee5187bf485f2f24ea5cab'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L10', 79000, 'AVAILABLE'),
    (UNHEX('da71789adf4b57b1bfdde15e0525ea82'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L11', 79000, 'AVAILABLE'),
    (UNHEX('abd9cd3a944e52c4840fc0144d6e258d'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L12', 79000, 'AVAILABLE'),
    (UNHEX('a88301e8d0745377b8e26df3cb9db27f'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L13', 79000, 'AVAILABLE'),
    (UNHEX('5a10879808a05a1e816446674838b142'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L14', 79000, 'AVAILABLE'),
    (UNHEX('2c726f9fe250502f93558500476d1602'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L15', 79000, 'AVAILABLE'),
    (UNHEX('536a19dc6c775bc69769646ddcb4d37b'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L16', 79000, 'AVAILABLE'),
    (UNHEX('00d29cab2f595e21af20275504260086'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'L17', 79000, 'AVAILABLE'),
    (UNHEX('fa0912e4540052e2b7070be50566b4b5'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M01', 168000, 'AVAILABLE'),
    (UNHEX('1396f5064e385e6cae970144c76e0446'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M02', 168000, 'AVAILABLE'),
    (UNHEX('bd5f433e9302553d967b8a48b1b4f859'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M03', 168000, 'AVAILABLE'),
    (UNHEX('c8949516994f5f95b1cb606a8bc3bace'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M04', 168000, 'AVAILABLE'),
    (UNHEX('5e6a032fc62e5dd58f0e84d9a40f8374'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M05', 168000, 'AVAILABLE'),
    (UNHEX('38eadda37b4155bb9dc4c974e7973445'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M06', 168000, 'AVAILABLE'),
    (UNHEX('a3dca3dc59ed5c24a8ea3abdd3696fe1'), UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('60c9aaf3f48d57e687a36f0a82d8d6f1'), 'M07', 168000, 'AVAILABLE'),
    (UNHEX('b778e2618b73505a9e8492561546249b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A01', 79000, 'AVAILABLE'),
    (UNHEX('5914f3f31e2557e69585c5095273dae7'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A02', 79000, 'AVAILABLE'),
    (UNHEX('4b279a47d61e56b49717e35bbf07d082'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A03', 79000, 'AVAILABLE'),
    (UNHEX('432694c4479d5cc09069777576bacc4e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A04', 79000, 'AVAILABLE'),
    (UNHEX('e254c9e55bc7554b85288ecc8109576f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A05', 79000, 'AVAILABLE'),
    (UNHEX('925a58b42b4b57f9a42d3667e65b4c6b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A06', 79000, 'AVAILABLE'),
    (UNHEX('9a67165622835d3e96acabe670f03ab7'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A07', 79000, 'AVAILABLE'),
    (UNHEX('df531298664e5c63a31274dc8b5ebd57'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A08', 79000, 'AVAILABLE'),
    (UNHEX('568cecc67fb65ceba5aa9f21bd9ed3f8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A09', 79000, 'AVAILABLE'),
    (UNHEX('913e42dde04e5cc2a52a25359f7a56f4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A10', 79000, 'AVAILABLE'),
    (UNHEX('d5fb225a90cc570baf38dc2ad7889a2a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A11', 79000, 'AVAILABLE'),
    (UNHEX('86024a51b9fc570ea73d9fb478034f2e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A12', 79000, 'AVAILABLE'),
    (UNHEX('43e234af21f250a9bb2bc699ecc79c32'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A13', 79000, 'AVAILABLE'),
    (UNHEX('c900e1cfd3385457a535f3ec4bbc52be'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A14', 79000, 'AVAILABLE'),
    (UNHEX('32ea7d4d6f6b5c858b6cc9cbf982b074'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'A15', 79000, 'AVAILABLE'),
    (UNHEX('31ec362f7fe750448b8ab075b7dc142e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B01', 79000, 'AVAILABLE'),
    (UNHEX('eb1ec0875def5f8088b7d18274b8916d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B02', 79000, 'AVAILABLE'),
    (UNHEX('a1bea5cdceaa5663a83f391876ebf19d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B03', 79000, 'AVAILABLE'),
    (UNHEX('cb2f1cac6de952d9ba8cd34e06796019'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B04', 79000, 'AVAILABLE'),
    (UNHEX('cf990f017bf75d5597cc154acb7b354b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B05', 79000, 'AVAILABLE'),
    (UNHEX('5aec0727fdf25d8c8fe33dbd81ed5fcd'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B06', 79000, 'AVAILABLE'),
    (UNHEX('18cbd6485e9f5a2bb6439f124585870c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B07', 79000, 'AVAILABLE'),
    (UNHEX('0f404ddcb64b5b8e87022b65601641d8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B08', 79000, 'AVAILABLE'),
    (UNHEX('57ae801870c3564a890df9096c59827f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B09', 79000, 'AVAILABLE'),
    (UNHEX('4c75dd8d30aa5a08850f8a2b586d4afd'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B10', 79000, 'AVAILABLE'),
    (UNHEX('02df41940e175d16bbba33becbd0d671'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B11', 79000, 'AVAILABLE'),
    (UNHEX('fdc507a7b448526c9bbfaee9804d7550'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B12', 79000, 'AVAILABLE'),
    (UNHEX('921c15e4f9a556e1b5b6fbddeb74dd76'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B13', 79000, 'AVAILABLE'),
    (UNHEX('37d3f881f5f75180ac2de6b30409985b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B14', 79000, 'AVAILABLE'),
    (UNHEX('495c49e83fcc5c9b8a10ae68b7d186dc'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'B15', 79000, 'AVAILABLE'),
    (UNHEX('07f7d25ec8d153c3afca5efb452e1e6e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C01', 79000, 'AVAILABLE'),
    (UNHEX('3e35cd79a3545410845b077b0714a8da'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C02', 79000, 'AVAILABLE'),
    (UNHEX('b233880c22bd5a1280c6422da2c0c7b5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C03', 79000, 'AVAILABLE'),
    (UNHEX('a0412e6fc47558eeb60122765aef9d06'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C04', 79000, 'AVAILABLE'),
    (UNHEX('e79567c46b6b5b1596f23929e436acbd'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C05', 79000, 'AVAILABLE'),
    (UNHEX('969f3ad82c8d574489e89d110bc99d3a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C06', 79000, 'AVAILABLE'),
    (UNHEX('1a44662ba6335ea08b839f7e62d052b2'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C07', 79000, 'AVAILABLE'),
    (UNHEX('8eb45cce7bf256619314003480d3f217'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C08', 79000, 'AVAILABLE'),
    (UNHEX('fbf480f038035aa0aa864714eb7ac0d0'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C09', 79000, 'AVAILABLE'),
    (UNHEX('08d0cd9f09255c8fbdc46dd6b6bfca49'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C10', 79000, 'AVAILABLE'),
    (UNHEX('8a00882d8590565da466dae3f24f3f5d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C11', 79000, 'AVAILABLE'),
    (UNHEX('c3fddecf8ef65bf5bb33e08e4c9cea2a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C12', 79000, 'AVAILABLE'),
    (UNHEX('ca8ce37f041c5a629d761845a2282103'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C13', 79000, 'AVAILABLE'),
    (UNHEX('3b9cfa44d2da5e37a75e6d9a2efb0a93'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C14', 79000, 'AVAILABLE'),
    (UNHEX('2993d3ac936b59d6b5306460e30fe66a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'C15', 79000, 'AVAILABLE'),
    (UNHEX('0e422cd41198520d829c0e55ece53ad4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D01', 79000, 'AVAILABLE'),
    (UNHEX('f8fc77bfbd83541a90eda76f2ed55846'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D02', 79000, 'AVAILABLE'),
    (UNHEX('e0195e9e72335329aecdd8a0b79bafb6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D03', 79000, 'AVAILABLE'),
    (UNHEX('48b0d40bd4a351d0b0b93ee66db3fb3b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D04', 79000, 'AVAILABLE'),
    (UNHEX('8c9d611974f55612939bacccf81420f1'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D05', 79000, 'AVAILABLE'),
    (UNHEX('aabba8611e35532eb9f28901cf157971'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D06', 79000, 'AVAILABLE'),
    (UNHEX('efbf723e46be56d79936a7c50df6c390'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D07', 79000, 'AVAILABLE'),
    (UNHEX('f3c62f70afea54498bd50db885e35101'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D08', 79000, 'AVAILABLE'),
    (UNHEX('27b9dfc0c61c5a99aed1c1bb85a16c23'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D09', 79000, 'AVAILABLE'),
    (UNHEX('0e3f3d0571495ea1a0cd97c9859a067c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D10', 79000, 'AVAILABLE'),
    (UNHEX('cec249af7d6752b38431c963a70d33e4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D11', 79000, 'AVAILABLE'),
    (UNHEX('3d5fe6268c2e5225847ff73eb0f4786f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D12', 79000, 'AVAILABLE'),
    (UNHEX('e5b2e9aaf17753cda8db97d4c873685b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D13', 79000, 'AVAILABLE'),
    (UNHEX('ecab92cce0ac5d19a2c7b2e5d2e1d314'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D14', 79000, 'AVAILABLE'),
    (UNHEX('bb10ddbe3f1d5830a0937d35ac733fcf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'D15', 79000, 'AVAILABLE'),
    (UNHEX('01da0de8950d5e9f83e38b0e42499dba'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E01', 79000, 'AVAILABLE'),
    (UNHEX('f97c26478d19554685c6b0954dea79b8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E02', 79000, 'AVAILABLE'),
    (UNHEX('e344a9030a9058ffa1db4601b85f570a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E03', 79000, 'AVAILABLE'),
    (UNHEX('4f838cd84ccc5a48bce87438638665f5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E04', 79000, 'AVAILABLE'),
    (UNHEX('155a1d51e12e53ec879072c9299b675f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E05', 79000, 'AVAILABLE'),
    (UNHEX('73dfb7c693245e3298a62dc3ce0e0aff'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E06', 79000, 'AVAILABLE'),
    (UNHEX('5030eed30ed55b9b98eff9cf4380df29'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E07', 79000, 'AVAILABLE'),
    (UNHEX('7bdb045c4e2950fe9beeceb56890aa27'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E08', 79000, 'AVAILABLE'),
    (UNHEX('a768b446aac9515d85476fe74b588dc4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E09', 79000, 'AVAILABLE'),
    (UNHEX('8f0599d1d3df519a9f7b84ec3b50d39c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E10', 79000, 'AVAILABLE'),
    (UNHEX('10399ac7e45258638088e9af257d3dd6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E11', 79000, 'AVAILABLE'),
    (UNHEX('556a7d140d8c5acd85a0973b81d0a16b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E12', 79000, 'AVAILABLE'),
    (UNHEX('fd03a3269cfe591abb3d18551148979a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E13', 79000, 'AVAILABLE'),
    (UNHEX('21ea04ddb804516d82c9f15c1c725219'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E14', 79000, 'AVAILABLE'),
    (UNHEX('595002bbb7f75c51ba1f85199c22e26f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'E15', 79000, 'AVAILABLE'),
    (UNHEX('d1605596c47453f7ae712562d7fc6a1c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F01', 79000, 'AVAILABLE'),
    (UNHEX('1ff94b81d3ce5619b1a514e3cb82c572'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F02', 79000, 'AVAILABLE'),
    (UNHEX('f0e045e53d1c5c53845f4f33cab38599'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F03', 79000, 'AVAILABLE'),
    (UNHEX('2a2cb8e4e6ec5eee8edc656187b9fac8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F04', 79000, 'AVAILABLE'),
    (UNHEX('cb519f2c892359a6b4a139ea8e266db2'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F05', 79000, 'AVAILABLE'),
    (UNHEX('db4e737a16a254f8966870d3cf108e88'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F06', 79000, 'AVAILABLE'),
    (UNHEX('617b9c115bdd5e1abac38cce9e57be1e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F07', 79000, 'AVAILABLE'),
    (UNHEX('d727aa36394c59658bffa6c258e32164'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F08', 79000, 'AVAILABLE'),
    (UNHEX('9ae98b4f81b85657a06e60dc133e2ef2'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F09', 79000, 'AVAILABLE'),
    (UNHEX('75dcaeb02cd35e298a0d47bf031fc66f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F10', 79000, 'AVAILABLE'),
    (UNHEX('eec087093c695ca984bb9774b8f9b267'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F11', 79000, 'AVAILABLE'),
    (UNHEX('2d62d65ba2f65ef49c9a5982cfe0b055'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F12', 79000, 'AVAILABLE'),
    (UNHEX('39f1d3602a735d26adceeac8211f6a8d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F13', 79000, 'AVAILABLE'),
    (UNHEX('a811356e510c5da38ca3af138230c255'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F14', 79000, 'AVAILABLE'),
    (UNHEX('75dbfc58ef455ea9b8bab90d2dc21b79'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'F15', 79000, 'AVAILABLE'),
    (UNHEX('a2697184d8fd53f0b0808d0d09786658'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G01', 79000, 'AVAILABLE'),
    (UNHEX('78571df1c69052a991dda6630213bd4f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G02', 79000, 'AVAILABLE'),
    (UNHEX('de090baf49035c12ba70f675a01a618a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G03', 79000, 'AVAILABLE'),
    (UNHEX('02342ba253645990a6d1984581184085'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G04', 79000, 'AVAILABLE'),
    (UNHEX('b32f7f2a25f45aab8697b28d0833b63c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G05', 79000, 'AVAILABLE'),
    (UNHEX('b0a004b188245a80b4bec197a01d454e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G06', 79000, 'AVAILABLE'),
    (UNHEX('e431da81aea353f8bb8d33ce0b2dc535'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G07', 79000, 'AVAILABLE'),
    (UNHEX('1c315d512c9e53f7818aeee2e963bb12'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G08', 79000, 'AVAILABLE'),
    (UNHEX('2e2fb58545c5553d9c4d686998e4993c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G09', 79000, 'AVAILABLE'),
    (UNHEX('c19468653d3a598f83c6d22b744208b8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G10', 79000, 'AVAILABLE'),
    (UNHEX('4a2d0cc5c221577aa8c5f6b9178dcccf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G11', 79000, 'AVAILABLE'),
    (UNHEX('108a586b76125f169be915c30de2a954'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G12', 79000, 'AVAILABLE'),
    (UNHEX('afee679fb3ad5e729ce22341fede66bf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G13', 79000, 'AVAILABLE'),
    (UNHEX('cfb42f2653915788a5b3c98bf0ca9c60'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G14', 79000, 'AVAILABLE'),
    (UNHEX('8476de469e6b5ad38e267fc1578a5084'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G15', 79000, 'AVAILABLE'),
    (UNHEX('688bffe51a2852c3ab0dfa3ed2476674'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G16', 79000, 'AVAILABLE'),
    (UNHEX('8ab04ded54e25c4486339b80df164a12'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'G17', 79000, 'AVAILABLE'),
    (UNHEX('672dbd4851db5d10b491c43996a69d09'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H01', 79000, 'AVAILABLE'),
    (UNHEX('6e749d30deb85670a23065b8c09fc95c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H02', 79000, 'AVAILABLE'),
    (UNHEX('eea26175d6325482b272d4a55fee7807'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H03', 79000, 'AVAILABLE'),
    (UNHEX('3cc0e320a81356be99f48fde04cd83ac'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H04', 79000, 'AVAILABLE'),
    (UNHEX('cee524bdde49503f97cee7cf9bb92edf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H05', 79000, 'AVAILABLE'),
    (UNHEX('ae979ef76a495ca486aa8959f4a7f601'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H06', 79000, 'AVAILABLE'),
    (UNHEX('47cc92ff4b6751a084bab0d1673c9a43'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H07', 79000, 'AVAILABLE'),
    (UNHEX('053953c59250573ebcae32398d52ebb8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H08', 79000, 'AVAILABLE'),
    (UNHEX('8725f53db30d57e4bf804f7e30a6219b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H09', 79000, 'AVAILABLE'),
    (UNHEX('4d30dc1058bb51058aee11400fa990ba'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H10', 79000, 'AVAILABLE'),
    (UNHEX('cf39b24788695371b57f7ffbe7fc066d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H11', 79000, 'AVAILABLE'),
    (UNHEX('e111fe51ff285d539ba22f8d1ee74a3c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H12', 79000, 'AVAILABLE'),
    (UNHEX('8ea58976c7e7569cbe3d5a071be4fe8c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H13', 79000, 'AVAILABLE'),
    (UNHEX('7eb4d4e210e954d5835f3fd63d3df20e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H14', 79000, 'AVAILABLE'),
    (UNHEX('9f50b77aac2b5396ad8a9efde3cf4317'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H15', 79000, 'AVAILABLE'),
    (UNHEX('a3aa408b8a59564ab94e753ba0813134'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H16', 79000, 'AVAILABLE'),
    (UNHEX('fe5704a345025d419366852bea17020b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'H17', 79000, 'AVAILABLE'),
    (UNHEX('f572e4f80a9c5785b9fd125b3953d521'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J01', 79000, 'AVAILABLE'),
    (UNHEX('a70b03de340d5c5a8b0990fd9a3dc9a7'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J02', 79000, 'AVAILABLE'),
    (UNHEX('788cf212f9845fa7a33f3917c39470dc'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J03', 79000, 'AVAILABLE'),
    (UNHEX('57b25ac4aa485afd983c3003956bc134'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J04', 79000, 'AVAILABLE'),
    (UNHEX('82c52e6305df5aa0ac7c8360cb38a09f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J05', 79000, 'AVAILABLE'),
    (UNHEX('3d607632bdd757618a7069f31249261c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J06', 79000, 'AVAILABLE'),
    (UNHEX('6b6fa90e47015b15a70c14c1ef8a1649'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J07', 79000, 'AVAILABLE'),
    (UNHEX('a14fd02695265bac8eeb8f9e2944dcdf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J08', 79000, 'AVAILABLE'),
    (UNHEX('6830deaa7b5e56b2b222903bfaf20f7b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J09', 79000, 'AVAILABLE'),
    (UNHEX('061a5dbd71a05e2da81f6084b4b31980'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J10', 79000, 'AVAILABLE'),
    (UNHEX('4496937146135b68b646a196e552d816'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J11', 79000, 'AVAILABLE'),
    (UNHEX('c39505ce273e5692b66f3cb9fbea6f84'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J12', 79000, 'AVAILABLE'),
    (UNHEX('171c0923ecec5ae88dc6e7962c02ad2f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J13', 79000, 'AVAILABLE'),
    (UNHEX('7eba096365a25ddd8aded6d4faf2d2a7'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J14', 79000, 'AVAILABLE'),
    (UNHEX('6e0a1443fee55cbbb32a326092df99e8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J15', 79000, 'AVAILABLE'),
    (UNHEX('bfc18002c6115b7fa1cf3e0bfc275ee3'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J16', 79000, 'AVAILABLE'),
    (UNHEX('e9f61afb6ce356de8b29c53a2508ffeb'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'J17', 79000, 'AVAILABLE'),
    (UNHEX('de6e28f7f91e5925ad0d235f932e7ad5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K01', 79000, 'AVAILABLE'),
    (UNHEX('9bd63518c86951d4b0c935d36464bca3'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K02', 79000, 'AVAILABLE'),
    (UNHEX('fef46d349c6f5570b54aef15d4a2412d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K03', 79000, 'AVAILABLE'),
    (UNHEX('92b093816ef1587a8dbd0aeb8b5342ce'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K04', 79000, 'AVAILABLE'),
    (UNHEX('1d79160a772852f283419cbf19cfbacf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K05', 79000, 'AVAILABLE'),
    (UNHEX('8ae66c5f32f8514cbf92a969853c81b9'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K06', 79000, 'AVAILABLE'),
    (UNHEX('27daf34de4e45bf39d83760dd1f2aba9'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K07', 79000, 'AVAILABLE'),
    (UNHEX('37f6a6089d615ba3b8ee2163538d4c20'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K08', 79000, 'AVAILABLE'),
    (UNHEX('98da0f4e09015d70998f738d77f44abb'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K09', 79000, 'AVAILABLE'),
    (UNHEX('35ebb3a2e9f758c597072a5fe508ac95'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K10', 79000, 'AVAILABLE'),
    (UNHEX('0f39beec264859278d4ade35589f8837'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K11', 79000, 'AVAILABLE'),
    (UNHEX('cd00e15897ec5b57b5027850df206c73'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K12', 79000, 'AVAILABLE'),
    (UNHEX('8e8dcb17d50d5a4dbfd44f4cb2c23b6c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K13', 79000, 'AVAILABLE'),
    (UNHEX('7b52cb91670157c6b101d4b2b84596ae'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K14', 79000, 'AVAILABLE'),
    (UNHEX('f0e36d0d2fd953988ac22ae9b5420d29'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K15', 79000, 'AVAILABLE'),
    (UNHEX('5bc9ab97074352a9b3c98cd3f7129ee6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'K16', 79000, 'AVAILABLE'),
    (UNHEX('9325722716595d0096b30642a4f2c844'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L01', 79000, 'AVAILABLE'),
    (UNHEX('02b0b13ab9295e81ac216157ad0fedb0'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L02', 79000, 'AVAILABLE'),
    (UNHEX('e2386f6302c353469c7872bc6630523b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L03', 79000, 'AVAILABLE'),
    (UNHEX('1799ebc3f6265cafb32ca282f4504596'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L04', 79000, 'AVAILABLE'),
    (UNHEX('352f994aa0a25bb1984cdba94f2e5923'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L05', 79000, 'AVAILABLE'),
    (UNHEX('d47c761342d55d7ba42fc4c86a6a1fd4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L06', 79000, 'AVAILABLE'),
    (UNHEX('db932b5dd9a05430bfec475a2490fb76'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L07', 79000, 'AVAILABLE'),
    (UNHEX('d7afaccbf69b5e319001c3d4a398b705'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L08', 79000, 'AVAILABLE'),
    (UNHEX('255c8cd68b345b48b178eba3a5eeb7c5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L09', 79000, 'AVAILABLE'),
    (UNHEX('16fb0292d2045b5a947daadfc7926154'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L10', 79000, 'AVAILABLE'),
    (UNHEX('f561fd57dee45b6e9f2e3677b4d6be8d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L11', 79000, 'AVAILABLE'),
    (UNHEX('23276ca4edfa5d43b82b95f2e80715c1'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L12', 79000, 'AVAILABLE'),
    (UNHEX('7c2b999e1cb55aa1906646f6b8d8ba94'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L13', 79000, 'AVAILABLE'),
    (UNHEX('98c7f2514a085323a11fb9c739d9aab8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L14', 79000, 'AVAILABLE'),
    (UNHEX('0a713f4ee78b51c9906bb12ba94273da'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L15', 79000, 'UNAVAILABLE'),
    (UNHEX('9b6bff2d461c5056a184adef4e272536'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L16', 79000, 'UNAVAILABLE'),
    (UNHEX('95108a34899155f296bcc4f5d17d5324'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'L17', 79000, 'AVAILABLE'),
    (UNHEX('c87ef0c5559e5fb2b68fb6f1770f7c54'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M01', 79000, 'AVAILABLE'),
    (UNHEX('0d01e2e527715d0bb82500d62e80e201'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M02', 79000, 'AVAILABLE'),
    (UNHEX('133ec3b16daa53a69fc01d82c0425ccb'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M03', 79000, 'AVAILABLE'),
    (UNHEX('c0862f5227f75764a479aeb682213bd5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M04', 79000, 'AVAILABLE'),
    (UNHEX('580224b28f8157888ce95e0a378f2618'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M05', 79000, 'AVAILABLE'),
    (UNHEX('4d6288984d16528fa479091479ed6754'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M06', 79000, 'AVAILABLE'),
    (UNHEX('724a5655f86d544da10da088145bd43b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M07', 79000, 'AVAILABLE'),
    (UNHEX('4a68f9d8006a59088f03b9de45dd8e46'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M08', 79000, 'AVAILABLE'),
    (UNHEX('985a6eabf0bb555f892a7ebe2276deb0'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M09', 79000, 'AVAILABLE'),
    (UNHEX('8a614bb163b9555d81b23159a99643c4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M10', 79000, 'AVAILABLE'),
    (UNHEX('f1f5b275dc865ce9a99c2a53ad0d3dad'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M11', 79000, 'AVAILABLE'),
    (UNHEX('10a8383bfdbb5b19825a44b41e6c2f5f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M12', 79000, 'AVAILABLE'),
    (UNHEX('0cc67b308bfc5043902c66297e46996b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M13', 79000, 'AVAILABLE'),
    (UNHEX('3f09d23627795a71b6f285cf57346878'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M14', 79000, 'AVAILABLE'),
    (UNHEX('dd1439eceb6e5c0a99ab3e38c9cf3d34'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M15', 79000, 'AVAILABLE'),
    (UNHEX('b9f0cc870415500b91be5398b3642140'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'M16', 79000, 'AVAILABLE'),
    (UNHEX('544e67c3517c5cd1a8b8afab79b184dd'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N01', 79000, 'AVAILABLE'),
    (UNHEX('33ee5144a42256bd9787495e0081ceab'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N02', 79000, 'AVAILABLE'),
    (UNHEX('593f96d1e99853f9adcaf8935af17998'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N03', 79000, 'AVAILABLE'),
    (UNHEX('227b2773ea76564d974e70f53f801364'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N04', 79000, 'AVAILABLE'),
    (UNHEX('b44bfadaac4b557cb98165d654be04a0'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N05', 79000, 'AVAILABLE'),
    (UNHEX('9590c63a084450f09ccec03086ce7dd6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N06', 79000, 'AVAILABLE'),
    (UNHEX('794d2863e119551ba869ea3617da3f5c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N07', 79000, 'AVAILABLE'),
    (UNHEX('851d1fe03f915caa9fafeab7c0fa3404'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N08', 79000, 'AVAILABLE'),
    (UNHEX('c8e557ddfc095b488ad7512feb8bbbf2'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N09', 79000, 'AVAILABLE'),
    (UNHEX('c2e50b7ba16c5099abdf296949b2b435'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N10', 79000, 'AVAILABLE'),
    (UNHEX('147481ba61ee59509e8654895a7e9a5e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N11', 79000, 'AVAILABLE'),
    (UNHEX('1b5a93c5f3fe5dd09765233c3f0c6632'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N12', 79000, 'AVAILABLE'),
    (UNHEX('b0f1a039f80851fd9a519036ab2e0473'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N13', 79000, 'AVAILABLE'),
    (UNHEX('5a1102aed8b75dff89a78e139e1158f8'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N14', 79000, 'AVAILABLE'),
    (UNHEX('6a0f650f0cbe506ca5ed32c6629dfdc9'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N15', 79000, 'AVAILABLE'),
    (UNHEX('4872180d38ba54b79d450abe3fe75450'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N16', 79000, 'AVAILABLE'),
    (UNHEX('f0b9b9f738d757fbbc823e58e6b9148e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'N17', 79000, 'AVAILABLE'),
    (UNHEX('08946bb4ca875978afebdac591022657'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O01', 79000, 'AVAILABLE'),
    (UNHEX('924d6ccc1d9852299ecd428fd604495e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O02', 79000, 'AVAILABLE'),
    (UNHEX('e374211b0744574b8a1c1bfa9e05e002'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O03', 79000, 'AVAILABLE'),
    (UNHEX('296cca8fb3b55e5495e3435d18d52f9c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O04', 79000, 'AVAILABLE'),
    (UNHEX('7d96270c724b591fb16e425f1191bbee'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O05', 79000, 'AVAILABLE'),
    (UNHEX('10f9dae4c7d9521388be4ac870f14dd6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O06', 79000, 'AVAILABLE'),
    (UNHEX('fcafcec590255b5baed26549abfa2047'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O07', 79000, 'AVAILABLE'),
    (UNHEX('a8f4da8400c9570d9e09f41add755abc'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O08', 79000, 'AVAILABLE'),
    (UNHEX('e1f113b0111152a6875a10b06a694f48'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O09', 79000, 'AVAILABLE'),
    (UNHEX('b63449f176185f42bd46a00e6ce2e1fd'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O10', 79000, 'AVAILABLE'),
    (UNHEX('40ea6779b48d565a82d5563fcc8fc3b9'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O11', 79000, 'AVAILABLE'),
    (UNHEX('b34aee1cec2d52e39476c9c8ffb110bb'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O12', 79000, 'AVAILABLE'),
    (UNHEX('9e512c032cd55b058d43009aa61a7a1b'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O13', 79000, 'AVAILABLE'),
    (UNHEX('6f744a3577815a3f82ac4023816ad632'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O14', 79000, 'AVAILABLE'),
    (UNHEX('37c79df6b6625f838330bb49bb630946'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O15', 79000, 'AVAILABLE'),
    (UNHEX('08597d497a235f76b689b7fef0eb6509'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'O16', 79000, 'AVAILABLE'),
    (UNHEX('2082d3bde8b8506e8494f0dbbc20cdad'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P01', 79000, 'AVAILABLE'),
    (UNHEX('94af3adc83ba5c3a87a480e3e5b14969'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P02', 79000, 'AVAILABLE'),
    (UNHEX('ba7c8ee483c55fdf9d780217a2272ccf'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P03', 79000, 'AVAILABLE'),
    (UNHEX('5c2348b96f165d57a71ab09ad4109a87'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P04', 79000, 'AVAILABLE'),
    (UNHEX('9ea8a9b2e9c054a596716d16e7fae11f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P05', 79000, 'AVAILABLE'),
    (UNHEX('68c11cf6af255ca3b723e410c77a0fdc'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P06', 79000, 'AVAILABLE'),
    (UNHEX('920344de58425be1b2ceffd582c6c4a4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P07', 79000, 'AVAILABLE'),
    (UNHEX('25e291e570485470840b598de9c6b3db'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P08', 79000, 'AVAILABLE'),
    (UNHEX('84bebca44f655fdeb681744bd7089e35'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P09', 79000, 'AVAILABLE'),
    (UNHEX('adad1a7b298451a19394839217982df4'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P10', 79000, 'AVAILABLE'),
    (UNHEX('25033f70852f5222b639b495b37bcdd5'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P11', 79000, 'AVAILABLE'),
    (UNHEX('c98df273f129554d96d1dd75642d6db9'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P12', 79000, 'AVAILABLE'),
    (UNHEX('399a789cdded5dfda3035cd911cb7d82'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P13', 79000, 'AVAILABLE'),
    (UNHEX('4f81748cfd1e51c9a4669fff352cc30a'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P14', 79000, 'AVAILABLE'),
    (UNHEX('209d2c00d852512f9292eb1efff9f59c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P15', 79000, 'AVAILABLE'),
    (UNHEX('3e8d4dab917359eca37b94dd7a52a594'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P16', 79000, 'AVAILABLE'),
    (UNHEX('ac4a21c42e2559c5a963532db603b770'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'P17', 79000, 'AVAILABLE'),
    (UNHEX('c3cf44160f945888b7322487c25a9cfa'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q01', 79000, 'AVAILABLE'),
    (UNHEX('3ff9fb6757335af8aa1f44895e344b8f'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q02', 79000, 'AVAILABLE'),
    (UNHEX('90cf9b54ea385420a757e030e9620312'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q03', 79000, 'AVAILABLE'),
    (UNHEX('3c81697b8f0a5159a2eb533c0c2cd2c3'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q04', 79000, 'AVAILABLE'),
    (UNHEX('a0334eb538ac58e8b5790ebe9f9b4239'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q05', 79000, 'AVAILABLE'),
    (UNHEX('0f700ff8b16450baa5615c12bd42647e'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q06', 79000, 'AVAILABLE'),
    (UNHEX('3a506f6439735736a6fc4fd46e00f3ed'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q07', 79000, 'AVAILABLE'),
    (UNHEX('c5845c1d4d2b5b7da6c7aacbe534f5e6'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q08', 79000, 'AVAILABLE'),
    (UNHEX('c264a7273c2c54eeab28b89a5151b289'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q09', 79000, 'AVAILABLE'),
    (UNHEX('768c3d66c2f95c7287c4a6e7ce38af48'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q10', 79000, 'AVAILABLE'),
    (UNHEX('c9e4bc5e6447555a98148667dba3c610'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q11', 79000, 'AVAILABLE'),
    (UNHEX('c56cc6b6f2255526999874430cf4ee42'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q12', 79000, 'AVAILABLE'),
    (UNHEX('9a2f9bbfef8c5891b2ad19170692f07d'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q13', 79000, 'AVAILABLE'),
    (UNHEX('230fbfa8128457cfa6b6fdfd49f36401'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'Q14', 79000, 'AVAILABLE'),
    (UNHEX('21e8910a879a5ffaa47d75e69b8a2c51'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R01', 168000, 'AVAILABLE'),
    (UNHEX('550713ae782e525b8cb05fc122f4e449'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R02', 168000, 'AVAILABLE'),
    (UNHEX('4387161ee904547883275a493ec2aa46'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R03', 168000, 'AVAILABLE'),
    (UNHEX('1cd134da296a5501ac20739dec166835'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R04', 168000, 'AVAILABLE'),
    (UNHEX('81b1531bdd52527b87cdef0e56a76824'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R05', 168000, 'AVAILABLE'),
    (UNHEX('0abcd5c70ec95ade9bb2fc33f7076a51'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'), UNHEX('75175e44310356b697054e8ff8d55d05'), 'R06', 168000, 'AVAILABLE');

START TRANSACTION;
SET @cinestar_qt_cinema = UNHEX('475ea861a4d55b4a87dc9df6c1853c92');
SET @cinestar_qt_movie = UNHEX('b03297fc2c9653f4906ecd30ae373087');
-- Guard names identify the prerequisite that failed if the CHECK rejects a row.
SET @cinestar_qt_room_matches = (
    SELECT COUNT(*) FROM cinestar_qt_rooms expected
    JOIN rooms r ON r.id = expected.id AND r.name = expected.name
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE c.id = @cinestar_qt_cinema AND c.active = TRUE AND r.active = TRUE AND r.room_type = 'STANDARD'
);
INSERT INTO cinestar_qt_guard VALUES ('base_seed_rooms04_05_exist_and_active', IF(@cinestar_qt_room_matches = 2, 1, 0));
SET @cinestar_qt_show_matches = (
    SELECT COUNT(*) FROM cinestar_qt_shows expected JOIN showtimes t ON t.id = expected.id
    WHERE t.room_id = expected.room_id AND t.movie_id = @cinestar_qt_movie
      AND t.starts_at = expected.starts_at AND t.ends_at = expected.ends_at
);
INSERT INTO cinestar_qt_guard VALUES ('base_seed_exact_two_showtimes_exist', IF(@cinestar_qt_show_matches = 2, 1, 0));
SET @cinestar_qt_layout_conflicts = (
    SELECT COUNT(*) FROM seats s JOIN cinestar_qt_rooms target ON target.id = s.room_id
    LEFT JOIN cinestar_qt_layout expected ON expected.room_id = s.room_id AND expected.seat_number = s.seat_number
    WHERE expected.id IS NULL OR s.row_label <> expected.row_label OR s.seat_type <> expected.seat_type OR s.active <> TRUE
);
INSERT INTO cinestar_qt_guard VALUES ('existing_physical_layouts_are_compatible', IF(@cinestar_qt_layout_conflicts = 0, 1, 0));
SET @cinestar_qt_price_conflicts = (
    SELECT COUNT(*) FROM show_seats ss JOIN cinestar_qt_shows target ON target.id = ss.showtime_id
    LEFT JOIN cinestar_qt_prices p ON p.showtime_id = ss.showtime_id AND p.seat_number = ss.seat_number
    LEFT JOIN seats s ON s.id = ss.seat_id
    LEFT JOIN cinestar_qt_layout l ON l.room_id = target.room_id AND l.seat_number = ss.seat_number
    WHERE p.id IS NULL OR l.id IS NULL OR s.id IS NULL
       OR s.room_id <> target.room_id OR s.seat_number <> ss.seat_number
       OR ss.seat_type <> l.seat_type OR ss.price <> p.price
);
INSERT INTO cinestar_qt_guard VALUES ('existing_show_seat_prices_are_compatible', IF(@cinestar_qt_price_conflicts = 0, 1, 0));

INSERT INTO seats (id, room_id, seat_number, row_label, seat_type, active, version, created_at, updated_at)
SELECT l.id, l.room_id, l.seat_number, l.row_label, l.seat_type, TRUE, 0, @cinestar_qt_now, @cinestar_qt_now
FROM cinestar_qt_layout l
LEFT JOIN seats existing ON existing.room_id = l.room_id AND existing.seat_number = l.seat_number
WHERE existing.id IS NULL;
SET @cinestar_qt_inserted_seats = ROW_COUNT();

INSERT INTO show_seats (id, showtime_id, seat_id, seat_number, seat_type, price, status, held_by_booking_id, hold_expires_at, version, created_at, updated_at)
SELECT p.id, t.id, s.id, s.seat_number, s.seat_type, p.price, p.snapshot_status, NULL, NULL, 0, @cinestar_qt_now, @cinestar_qt_now
FROM cinestar_qt_prices p JOIN showtimes t ON t.id = p.showtime_id AND t.room_id = p.room_id
JOIN seats s ON s.room_id = t.room_id AND s.seat_number = p.seat_number
LEFT JOIN show_seats existing ON existing.showtime_id = t.id AND (existing.seat_id = s.id OR existing.seat_number = s.seat_number)
WHERE existing.id IS NULL AND t.status IN ('SCHEDULED','OPEN_FOR_BOOKING')
  AND t.starts_at > UTC_TIMESTAMP(6) AND s.active = TRUE;
SET @cinestar_qt_inserted_show_seats = ROW_COUNT();

UPDATE showtimes t
JOIN cinestar_qt_shows expected ON expected.id = t.id
JOIN rooms r ON r.id = t.room_id JOIN cinemas c ON c.id = r.cinema_id
JOIN (SELECT showtime_id, COUNT(*) AS seat_count, SUM(status = 'AVAILABLE') AS available_count
      FROM show_seats WHERE showtime_id IN (UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931')) GROUP BY showtime_id) inventory ON inventory.showtime_id = t.id
SET t.status = 'OPEN_FOR_BOOKING', t.version = t.version + 1, t.updated_at = UTC_TIMESTAMP(6)
WHERE t.status = 'SCHEDULED' AND t.starts_at > UTC_TIMESTAMP(6)
  AND t.room_id = expected.room_id AND t.movie_id = @cinestar_qt_movie
  AND r.active = TRUE AND c.active = TRUE
  AND inventory.seat_count = expected.seat_count AND inventory.available_count > 0;
SET @cinestar_qt_opened_showtimes = ROW_COUNT();
COMMIT;

SELECT @cinestar_qt_inserted_seats AS inserted_seats, @cinestar_qt_inserted_show_seats AS inserted_show_seats, @cinestar_qt_opened_showtimes AS opened_showtimes;
SELECT BIN_TO_UUID(r.id,0) AS room_id, r.name AS room_name, s.seat_type, COUNT(*) AS seat_units
FROM rooms r JOIN cinestar_qt_rooms target ON target.id = r.id JOIN seats s ON s.room_id = r.id
GROUP BY r.id, r.name, s.seat_type ORDER BY r.name, s.seat_type;
SELECT BIN_TO_UUID(t.id,0) AS showtime_id, r.name AS room_name,
       t.starts_at AS starts_at_utc, DATE_ADD(t.starts_at, INTERVAL 7 HOUR) AS starts_at_cinema_local,
       t.status AS showtime_status, ss.seat_type, ss.price AS adult_price_vnd, ss.status AS seat_status, COUNT(ss.id) AS seat_units
FROM showtimes t JOIN rooms r ON r.id = t.room_id LEFT JOIN show_seats ss ON ss.showtime_id = t.id
WHERE t.id IN (UNHEX('465303c49455583c8c3df9370ac54e4c'), UNHEX('d19d2273c78d50b5b67b28699f8ec931'))
GROUP BY t.id, r.name, t.starts_at, t.status, ss.seat_type, ss.price, ss.status
ORDER BY t.starts_at, ss.seat_type, ss.status;
DROP TEMPORARY TABLE cinestar_qt_prices;
DROP TEMPORARY TABLE cinestar_qt_layout;
DROP TEMPORARY TABLE cinestar_qt_shows;
DROP TEMPORARY TABLE cinestar_qt_rooms;
DROP TEMPORARY TABLE cinestar_qt_guard;
SET SESSION time_zone = @cinestar_qt_previous_timezone;

-- Read-only manual checks; Inventory default port 8083, verify your deployment port:
-- GET /api/v1/showtimes/bookable?cinemaId=475ea861-a4d5-5b4a-87dc-9df6c1853c92&from=2026-10-09T00:00:00%2B07:00&to=2026-10-10T00:00:00%2B07:00&movieId=b03297fc-2c96-53f4-906e-cd30ae373087
-- GET /api/v1/seats?roomId=60c9aaf3-f48d-57e6-87a3-6f0a82d8d6f1
-- GET /api/v1/show-seats?showtimeId=465303c4-9455-583c-8c3d-f9370ac54e4c&availableOnly=false
-- GET /api/v1/seats?roomId=75175e44-3103-56b6-9705-4e8ff8d55d05
-- GET /api/v1/show-seats?showtimeId=d19d2273-c78d-50b5-b67b-28699f8ec931&availableOnly=false
