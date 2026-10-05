-- CINESTAR QUOC THANH / ROOM 06 / VERIFIED PUBLIC BOOKING SNAPSHOT
-- Generated UTC: 2026-10-05T04:00:41.277092+00:00
-- Backend main: a3fd0fc0fe422e8b655ffdd47a11161b2bb850df
-- Source: https://cinestar.com.vn/movie/8907c33a-0f25-48cb-ba66-caca866ab99a/
-- UI: Ho Chi Minh -> Cinestar Quoc Thanh -> Standard -> 18:50 -> 05/10 and 06/10.
-- Movie: QUYẾT CUA ANH NÀY! (PĐ) (T13). Room label: CHON GHE - Rap 06.
-- Source cinema_id: 8f3a5832-8340-4a43-89bc-6653817162f1; room_id: 61; room_name: 06.
-- PRICE SOURCE: Nguoi Lon / DON and Nguoi Lon / DOI; VND per seat unit.
-- 2026-10-05: DON 45000; DOI 100000. 2026-10-06: DON 75000; DOI 160000.
-- COUPLE is one seat unit for two people. Its stored price is NOT per-person.
-- This imports ONE verified room and TWO verified showtimes, not every cinema/showtime.
-- Run AFTER cinestar_seed.sql. Flyway must already have created the schema.
-- MySQL 8.0.16+; use one connection; stop on error; do not use --force.
-- Default schema: cinema_inventory_db. Change the USE below if necessary.
-- This SQL is a LOCAL DEVELOPMENT snapshot. It does not create bookings at Cinestar.
-- No live availability synchronization. Do not use this as inventory for selling Cinestar tickets.
-- Visible booked cells -> local UNAVAILABLE, not BOOKED: no fictional booking/account.
-- Other cells -> AVAILABLE only for this frozen local snapshot.
-- Reruns add missing rows; NEVER overwrite an existing seat, price, hold or booking state.
-- Guard rejects unexpected layouts/prices. Stop, inspect the mismatch; do not remove the guard.
-- Only the two matching future SCHEDULED showtimes are opened, after complete seat inventory exists.
-- Expired/CLOSED/CANCELLED/COMPLETED showtimes are not reopened or shifted to a new date.
-- Existing local IDs must match the stable IDs from cinestar_seed.sql.
-- Single-seat legend is Ghe Thuong -> STANDARD; Ghe Doi (2 Nguoi) -> COUPLE.
-- Some cells have CSS seat-vip, but the visible ticket contract has no separate VIP price.
-- No VIP 1.30 multiplier is invented. Prices below are observed adult ticket prices.
-- Current SeatPricingPolicy differs (VIP x1.30, COUPLE x2.00); this snapshot uses explicit prices.
-- No backend pricing-policy change is included. Do not regenerate these prices from a basePrice.
-- Rows/columns/aisles from the source are preserved below in comments and TEMP staging only.
-- Current seats table has no layout-column/aisle fields; the existing API cannot reproduce every gap.
-- End times remain those in the original seed (starts_at + 115 minutes), not official Cinestar end times.
-- Max additions: 88 physical seats; 176 show_seats; max 2 SCHEDULED -> OPEN_FOR_BOOKING transitions.
-- No persistent DDL, migration edits, promotions, membership, student discount rules or auth changes.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @cinestar_booking_previous_time_zone = @@SESSION.time_zone;
SET SESSION time_zone = '+00:00';
SET @cinestar_booking_now = UTC_TIMESTAMP(6);
USE cinema_inventory_db;
DROP TEMPORARY TABLE IF EXISTS cinestar_booking_guard;
DROP TEMPORARY TABLE IF EXISTS cinestar_booking_layout;
DROP TEMPORARY TABLE IF EXISTS cinestar_booking_prices;

-- Temporary tables vanish when the connection closes. They do not alter application schema.
CREATE TEMPORARY TABLE cinestar_booking_guard (
    guard_name VARCHAR(100) NOT NULL PRIMARY KEY,
    ok INT NOT NULL,
    CHECK (ok = 1)
) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_booking_layout (
    seed_id BINARY(16) NOT NULL PRIMARY KEY,
    seat_number VARCHAR(20) NOT NULL UNIQUE,
    row_label VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50) NOT NULL,
    source_column INT NOT NULL
) ENGINE=InnoDB;
CREATE TEMPORARY TABLE cinestar_booking_prices (
    seed_id BINARY(16) NOT NULL PRIMARY KEY,
    showtime_id BINARY(16) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    snapshot_status VARCHAR(30) NOT NULL,
    UNIQUE (showtime_id, seat_number)
) ENGINE=InnoDB;

INSERT INTO cinestar_booking_layout (seed_id, seat_number, row_label, seat_type, source_column) VALUES
    -- A01: source column 5
    (UNHEX('61451c5f296350648964901900b8081f'), 'A01', 'A', 'STANDARD', 5),
    -- A02: source column 6
    (UNHEX('bdf6bbf50bd052f88cd5421825c657a0'), 'A02', 'A', 'STANDARD', 6),
    -- A03: source column 7
    (UNHEX('424f06de3072584590a45bdf0da2a4c4'), 'A03', 'A', 'STANDARD', 7),
    -- A04: source column 8
    (UNHEX('dc875d9518795cffbe84b7e7fee1b3ab'), 'A04', 'A', 'STANDARD', 8),
    -- A05: source column 9
    (UNHEX('7e6672fd8cf755048ce68d9e64a017ca'), 'A05', 'A', 'STANDARD', 9),
    -- A06: source column 10
    (UNHEX('6e73010c37cf5a10a0f886afe6cfe5e1'), 'A06', 'A', 'STANDARD', 10),
    -- A07: source column 11
    (UNHEX('02571b33ed455c848d68291af390bf72'), 'A07', 'A', 'STANDARD', 11),
    -- A08: source column 12
    (UNHEX('d3b89cc0897d5e859fca984a6d98f68e'), 'A08', 'A', 'STANDARD', 12),
    -- B01: source column 5
    (UNHEX('75859a4b4a94521780f189a50e28fe56'), 'B01', 'B', 'STANDARD', 5),
    -- B02: source column 6
    (UNHEX('627c45f629905d6ba397f470905f62cf'), 'B02', 'B', 'STANDARD', 6),
    -- B03: source column 7
    (UNHEX('61581df536d75f0babf7d2eafef66d48'), 'B03', 'B', 'STANDARD', 7),
    -- B04: source column 8
    (UNHEX('d636906c371d55d3bdb6b836e1023751'), 'B04', 'B', 'STANDARD', 8),
    -- B05: source column 9
    (UNHEX('1d769fb7703456179c162e8c6fa99832'), 'B05', 'B', 'STANDARD', 9),
    -- B06: source column 10
    (UNHEX('292b85db866154ba9a56b25109ae3e48'), 'B06', 'B', 'STANDARD', 10),
    -- B07: source column 11
    (UNHEX('b6997a6469965e81bda1985cd88dad31'), 'B07', 'B', 'STANDARD', 11),
    -- B08: source column 12
    (UNHEX('3288203bfaff5ad8bc975d649be0f61e'), 'B08', 'B', 'STANDARD', 12),
    -- C01: source column 5
    (UNHEX('583725b14cfb5ee496f906e27a12042b'), 'C01', 'C', 'STANDARD', 5),
    -- C02: source column 6
    (UNHEX('35313dc6a411517090ee7bb2123b0109'), 'C02', 'C', 'STANDARD', 6),
    -- C03: source column 7
    (UNHEX('d2faa7de33af5674906f52cdf3c13cd5'), 'C03', 'C', 'STANDARD', 7),
    -- C04: source column 8
    (UNHEX('81c8ccef3c445e19bc07fb0d21fd81e5'), 'C04', 'C', 'STANDARD', 8),
    -- C05: source column 9
    (UNHEX('7249473c8971511283164a0ed0d0d309'), 'C05', 'C', 'STANDARD', 9),
    -- C06: source column 10
    (UNHEX('2f78555d98a759cc9a61a726806eb2f5'), 'C06', 'C', 'STANDARD', 10),
    -- C07: source column 11
    (UNHEX('b03ef03919bf56268e338d29ef74ff47'), 'C07', 'C', 'STANDARD', 11),
    -- C08: source column 12
    (UNHEX('bda75ac14083502e9e8bd568e2a44e66'), 'C08', 'C', 'STANDARD', 12),
    -- D01: source column 5
    (UNHEX('50fc14ccb59a50bda4ccd159c999069a'), 'D01', 'D', 'STANDARD', 5),
    -- D02: source column 6
    (UNHEX('e19e764b658f53fea343c10f98d7d9b3'), 'D02', 'D', 'STANDARD', 6),
    -- D03: source column 7
    (UNHEX('e727fd8751845a4b8d0e6f86775348da'), 'D03', 'D', 'STANDARD', 7),
    -- D04: source column 8
    (UNHEX('68dcf6c21ab957c4b205c7bca5f60f99'), 'D04', 'D', 'STANDARD', 8),
    -- D05: source column 9
    (UNHEX('b96edc08868f5b77956b593bb4ded3cf'), 'D05', 'D', 'STANDARD', 9),
    -- D06: source column 10
    (UNHEX('682e3e88b2c85cc4bb75e6fe75ad0a46'), 'D06', 'D', 'STANDARD', 10),
    -- D07: source column 11
    (UNHEX('2d5628ac671e502ab349f767882b3bac'), 'D07', 'D', 'STANDARD', 11),
    -- D08: source column 12
    (UNHEX('d8ff609cbc13548e99f8684c97214938'), 'D08', 'D', 'STANDARD', 12),
    -- E01: source column 5; CSS seat-vip; visible single-seat legend
    (UNHEX('0e2bf9ecc49c54f3a9cb2a7bfcba3bd0'), 'E01', 'E', 'STANDARD', 5),
    -- E02: source column 6; CSS seat-vip; visible single-seat legend
    (UNHEX('b4e0073204d15d44a7f04065b23707cf'), 'E02', 'E', 'STANDARD', 6),
    -- E03: source column 7; CSS seat-vip; visible single-seat legend
    (UNHEX('8e619f266d1652389913b4605d402b5d'), 'E03', 'E', 'STANDARD', 7),
    -- E04: source column 8; CSS seat-vip; visible single-seat legend
    (UNHEX('6278a3efd0485b38bef17fe0eeffd50c'), 'E04', 'E', 'STANDARD', 8),
    -- E05: source column 9; CSS seat-vip; visible single-seat legend
    (UNHEX('37ae31c2ef53571aa03c0b09c59f99d5'), 'E05', 'E', 'STANDARD', 9),
    -- E06: source column 10
    (UNHEX('5b60521feb125d2d875d51c570d1c7d9'), 'E06', 'E', 'STANDARD', 10),
    -- E07: source column 11
    (UNHEX('3a5563f873fb52638635afc482eac30e'), 'E07', 'E', 'STANDARD', 11),
    -- E08: source column 12
    (UNHEX('a08b90b6ba965b66be401f8eff14712b'), 'E08', 'E', 'STANDARD', 12),
    -- F01: source column 5; CSS seat-vip; visible single-seat legend
    (UNHEX('d9157f8f5ee35f0199f8d0a0537420e1'), 'F01', 'F', 'STANDARD', 5),
    -- F02: source column 6; CSS seat-vip; visible single-seat legend
    (UNHEX('e99f6b28b2d152e1863d088edec72e6f'), 'F02', 'F', 'STANDARD', 6),
    -- F03: source column 7; CSS seat-vip; visible single-seat legend
    (UNHEX('9555c0807cde538bbdc908791aa5065d'), 'F03', 'F', 'STANDARD', 7),
    -- F04: source column 8; CSS seat-vip; visible single-seat legend
    (UNHEX('66df1f884c2455228caba82bd5f818e3'), 'F04', 'F', 'STANDARD', 8),
    -- F05: source column 9; CSS seat-vip; visible single-seat legend
    (UNHEX('003cc5592c06596cba3370b5d6ced55f'), 'F05', 'F', 'STANDARD', 9),
    -- F06: source column 10
    (UNHEX('082e4fbe635550faa4d5b4947229554f'), 'F06', 'F', 'STANDARD', 10),
    -- F07: source column 11
    (UNHEX('50230cfb6e9251029d1d9b858dc6ee67'), 'F07', 'F', 'STANDARD', 11),
    -- F08: source column 12
    (UNHEX('f736f4958cf7557b9b1ed465a136e49b'), 'F08', 'F', 'STANDARD', 12),
    -- G01: source column 1
    (UNHEX('3656ba56d12a584c892b140a92423404'), 'G01', 'G', 'STANDARD', 1),
    -- G02: source column 2
    (UNHEX('a8c44344464b58d1a827d1bcd821cbf1'), 'G02', 'G', 'STANDARD', 2),
    -- G03: source column 5; CSS seat-vip; visible single-seat legend
    (UNHEX('ae65f8e23c58527faa64791ea43e08a2'), 'G03', 'G', 'STANDARD', 5),
    -- G04: source column 6; CSS seat-vip; visible single-seat legend
    (UNHEX('d8185b0c213257069657eca8925fd860'), 'G04', 'G', 'STANDARD', 6),
    -- G05: source column 7; CSS seat-vip; visible single-seat legend
    (UNHEX('47bbc0ae0b735a5693483b4dcec6da4a'), 'G05', 'G', 'STANDARD', 7),
    -- G06: source column 8; CSS seat-vip; visible single-seat legend
    (UNHEX('21db0b02cace505aba849d919d2b2f38'), 'G06', 'G', 'STANDARD', 8),
    -- G07: source column 9; CSS seat-vip; visible single-seat legend
    (UNHEX('bfbe81e5f18b5fb9b8109e367b86ae03'), 'G07', 'G', 'STANDARD', 9),
    -- G08: source column 10
    (UNHEX('a356b86b8d6555078b302e241d5b88a8'), 'G08', 'G', 'STANDARD', 10),
    -- G09: source column 11
    (UNHEX('5375a79a624f542c85f59ac246d2cc59'), 'G09', 'G', 'STANDARD', 11),
    -- G10: source column 12
    (UNHEX('3eb82ea0ac0a5150a9cc6c8c5363e732'), 'G10', 'G', 'STANDARD', 12),
    -- H01: source column 1
    (UNHEX('8ce769e6769258958758335cfddc768f'), 'H01', 'H', 'STANDARD', 1),
    -- H02: source column 2
    (UNHEX('67fd9cb5a1fb59cd9e6bda250f9c0a1c'), 'H02', 'H', 'STANDARD', 2),
    -- H03: source column 5; CSS seat-vip; visible single-seat legend
    (UNHEX('d4ac0512e6985438b9baf60120a16ee4'), 'H03', 'H', 'STANDARD', 5),
    -- H04: source column 6; CSS seat-vip; visible single-seat legend
    (UNHEX('6ee3ddba2e77590891db5cc4db7e27ec'), 'H04', 'H', 'STANDARD', 6),
    -- H05: source column 7; CSS seat-vip; visible single-seat legend
    (UNHEX('5cd6714926375659a7fd1c0d1b3cb575'), 'H05', 'H', 'STANDARD', 7),
    -- H06: source column 8; CSS seat-vip; visible single-seat legend
    (UNHEX('e092498ba7165ec9927a023a1396b7f3'), 'H06', 'H', 'STANDARD', 8),
    -- H07: source column 9; CSS seat-vip; visible single-seat legend
    (UNHEX('5599f8a239a6585880cc921d69af87cc'), 'H07', 'H', 'STANDARD', 9),
    -- H08: source column 10
    (UNHEX('b04e0caae0915680903f4e07aa01bdb7'), 'H08', 'H', 'STANDARD', 10),
    -- H09: source column 11
    (UNHEX('1e6755ee30da5fe4bc724e4babd8282a'), 'H09', 'H', 'STANDARD', 11),
    -- H10: source column 12
    (UNHEX('5c14ab47b7505dae81fda1f0d4c5b898'), 'H10', 'H', 'STANDARD', 12),
    -- J01: source column 1
    (UNHEX('9516b7072d02559f8b0b09b30a78b267'), 'J01', 'J', 'STANDARD', 1),
    -- J02: source column 2
    (UNHEX('d5e23032d7a05477a94b9e6d6f2fc928'), 'J02', 'J', 'STANDARD', 2),
    -- J03: source column 5; CSS seat-vip; visible single-seat legend
    (UNHEX('be374dce9e065e2b9d35a064f1d6e3e6'), 'J03', 'J', 'STANDARD', 5),
    -- J04: source column 6; CSS seat-vip; visible single-seat legend
    (UNHEX('1f0d09d06775529ea37579d39b97c2e0'), 'J04', 'J', 'STANDARD', 6),
    -- J05: source column 7; CSS seat-vip; visible single-seat legend
    (UNHEX('80b82425223850ebb27424e19527a17c'), 'J05', 'J', 'STANDARD', 7),
    -- J06: source column 8; CSS seat-vip; visible single-seat legend
    (UNHEX('77e553e27d595caeacf2b60c5d5e67c4'), 'J06', 'J', 'STANDARD', 8),
    -- J07: source column 9; CSS seat-vip; visible single-seat legend
    (UNHEX('0286052935265398942188849bbc4a60'), 'J07', 'J', 'STANDARD', 9),
    -- J08: source column 10
    (UNHEX('fddaca1241b651888e6d0ee0b59dff08'), 'J08', 'J', 'STANDARD', 10),
    -- J09: source column 11
    (UNHEX('4f439d89b97b5877ba2cce5925d229e5'), 'J09', 'J', 'STANDARD', 11),
    -- J10: source column 12
    (UNHEX('491b04084e145dfc8590b47cf1d8f29b'), 'J10', 'J', 'STANDARD', 12),
    -- K01: source column 1
    (UNHEX('8ae8b5ce233f5898a49f903cb6f8c656'), 'K01', 'K', 'STANDARD', 1),
    -- K02: source column 2
    (UNHEX('ac4801f497c75914bfde1baa1ce672f3'), 'K02', 'K', 'STANDARD', 2),
    -- L01: source column 1
    (UNHEX('1bc1ee3653315b04967e1487613224b2'), 'L01', 'L', 'STANDARD', 1),
    -- L02: source column 2
    (UNHEX('931d36d24e2b50d8bfffb2206cdbc940'), 'L02', 'L', 'STANDARD', 2),
    -- L03: source column 5
    (UNHEX('58a24de247485d40881ff50e93a76506'), 'L03', 'L', 'STANDARD', 5),
    -- L04: source column 6
    (UNHEX('f0c74e95762b5a0e9c112c82b0759e26'), 'L04', 'L', 'STANDARD', 6),
    -- M01: source column 1
    (UNHEX('171fb28d9f6f55aa99ca3655a45d136e'), 'M01', 'M', 'COUPLE', 1),
    -- M02: source column 4
    (UNHEX('e6b977bd53805e26bd60297ee4e8b113'), 'M02', 'M', 'COUPLE', 4),
    -- N01: source column 1
    (UNHEX('1659aa2bede75950b28cdef95af53141'), 'N01', 'N', 'COUPLE', 1),
    -- N02: source column 2
    (UNHEX('766f13ee5c1f5bd782bf459209f5f43a'), 'N02', 'N', 'COUPLE', 2);

INSERT INTO cinestar_booking_prices (seed_id, showtime_id, seat_number, price, snapshot_status) VALUES
    (UNHEX('848259297e9d5b2da7dfc61aae04dd2f'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A01', 45000, 'AVAILABLE'),
    (UNHEX('4c83517cbba35c4eaf8e2aeb615ba8e6'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A02', 45000, 'AVAILABLE'),
    (UNHEX('2cad5801e4c0535f97330ccc803974a2'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A03', 45000, 'AVAILABLE'),
    (UNHEX('f0d31c3902585d6fb3c573419ac888f3'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A04', 45000, 'AVAILABLE'),
    (UNHEX('1d771f80028f551cb697c8c055268e5a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A05', 45000, 'AVAILABLE'),
    (UNHEX('42b3adfd82545d54be2b9be942d4a20e'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A06', 45000, 'AVAILABLE'),
    (UNHEX('47346d6a0ba85d96a94eac87daa6eb94'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A07', 45000, 'AVAILABLE'),
    (UNHEX('7b0aa1ec28d058d4aecb3a95f0cd1c19'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'A08', 45000, 'AVAILABLE'),
    (UNHEX('597f434e76a4562382b04965d09caf9d'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B01', 45000, 'AVAILABLE'),
    (UNHEX('b91f0fea727b596ab6c133d1f1c1b253'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B02', 45000, 'AVAILABLE'),
    (UNHEX('715fa536e3215a93b26b43490c55c6de'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B03', 45000, 'AVAILABLE'),
    (UNHEX('254793cbc9fd53aabcf1590419c86f6e'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B04', 45000, 'AVAILABLE'),
    (UNHEX('6fde8c6e7ac35062a32db4584f1ba076'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B05', 45000, 'AVAILABLE'),
    (UNHEX('9e72d7a87763570d988c5186695b4edc'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B06', 45000, 'AVAILABLE'),
    (UNHEX('4c8e9f2d55df5617ae2f30c405946d64'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B07', 45000, 'AVAILABLE'),
    (UNHEX('9f1fce7babe15485a49084c73a9f9d13'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'B08', 45000, 'AVAILABLE'),
    (UNHEX('eca946cc51fa5c04aac8109a9c9b1b1b'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C01', 45000, 'AVAILABLE'),
    (UNHEX('733ea82af0af56aca18ffb20438bcdaf'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C02', 45000, 'AVAILABLE'),
    (UNHEX('e46cfb87ca3a5e17901703ab3f521a15'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C03', 45000, 'AVAILABLE'),
    (UNHEX('1b43acb24cd25df0b095ce5a3cfbccf8'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C04', 45000, 'AVAILABLE'),
    (UNHEX('5a309908c2eb5bc9b24b4485920c87f2'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C05', 45000, 'AVAILABLE'),
    (UNHEX('5aaf155b11bf5cc499d1cc86fb714d0c'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C06', 45000, 'AVAILABLE'),
    (UNHEX('ba435d9bd5955e8a844cb90bee2d2c50'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C07', 45000, 'AVAILABLE'),
    (UNHEX('8a45dbd3e6f05521b2aae92576c86b63'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'C08', 45000, 'AVAILABLE'),
    (UNHEX('59557e6f67665983912a8ffeba44b42b'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D01', 45000, 'AVAILABLE'),
    (UNHEX('7e20a15e85ee5b35b7c9f9fbed2602fb'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D02', 45000, 'AVAILABLE'),
    (UNHEX('6ded36fb742f5d44a77edf0da07b44d8'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D03', 45000, 'AVAILABLE'),
    (UNHEX('a31752f88e655dbca026527b385cbf95'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D04', 45000, 'AVAILABLE'),
    (UNHEX('a0e30d6748e95b29af2713f81375a8c8'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D05', 45000, 'AVAILABLE'),
    (UNHEX('c3a471b65f1b5e268171f7370424c8dd'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D06', 45000, 'AVAILABLE'),
    (UNHEX('8b685352f607510794b1de8837eefb15'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D07', 45000, 'AVAILABLE'),
    (UNHEX('b7f6343754105784bb28ad0c05f9352a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'D08', 45000, 'AVAILABLE'),
    (UNHEX('65e15f6528355baca4727d0d8af8c2bb'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E01', 45000, 'AVAILABLE'),
    (UNHEX('2b07686cee98511ca27962b0eedbaf9c'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E02', 45000, 'AVAILABLE'),
    (UNHEX('48cabba54c2d5749a42bf96deef285e8'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E03', 45000, 'AVAILABLE'),
    (UNHEX('2f0d945135775e8c93622bab7854b958'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E04', 45000, 'UNAVAILABLE'),
    (UNHEX('00686822bd6252988e94add2f5a0a45a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E05', 45000, 'UNAVAILABLE'),
    (UNHEX('8e3b99df03605c5b999f0dfce25cc02a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E06', 45000, 'UNAVAILABLE'),
    (UNHEX('2371c131de0252dc88126b912da5638a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E07', 45000, 'AVAILABLE'),
    (UNHEX('30222edd431c590cbc9efe8792f2dc13'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'E08', 45000, 'AVAILABLE'),
    (UNHEX('3f100b5cda4b5aafb6bd49d562d29794'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F01', 45000, 'AVAILABLE'),
    (UNHEX('4320a6eef44f5148a8347454943ab95b'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F02', 45000, 'AVAILABLE'),
    (UNHEX('c55b8a34196154f88d5beff5bf059db2'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F03', 45000, 'UNAVAILABLE'),
    (UNHEX('79671f446cce598ebca9fa4de9381b71'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F04', 45000, 'AVAILABLE'),
    (UNHEX('e65d200ab2395a9eac6987631a71a8fd'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F05', 45000, 'AVAILABLE'),
    (UNHEX('1ec8457467e15520b513e9361fb0a2da'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F06', 45000, 'AVAILABLE'),
    (UNHEX('6d72b835a2825976b922ee31a27fc841'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F07', 45000, 'AVAILABLE'),
    (UNHEX('a2055a1d4d7958f79a9ae9840e12fb4d'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'F08', 45000, 'AVAILABLE'),
    (UNHEX('354a835461a55b37966ae2134ea8d24a'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G01', 45000, 'AVAILABLE'),
    (UNHEX('246ce61f752955809d354b4b268609ae'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G02', 45000, 'AVAILABLE'),
    (UNHEX('988d7ee8a3f754dcbc9b7b767100b1f5'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G03', 45000, 'UNAVAILABLE'),
    (UNHEX('46d2afec79985962a297720902ce9fb3'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G04', 45000, 'AVAILABLE'),
    (UNHEX('c8a9a568e28658ddbff5360a50d1f2f9'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G05', 45000, 'AVAILABLE'),
    (UNHEX('565e451a64e252ff95210c71b4a0f929'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G06', 45000, 'AVAILABLE'),
    (UNHEX('55dc22d11b7353b8a5d341d4875d92ff'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G07', 45000, 'AVAILABLE'),
    (UNHEX('2f45dd9649ec5af0a621776e76578d4f'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G08', 45000, 'AVAILABLE'),
    (UNHEX('77f439fea1c85bbd996e04a7a557adf0'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G09', 45000, 'AVAILABLE'),
    (UNHEX('c1aa9ae1204d52e68a99a798d3faadf3'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'G10', 45000, 'AVAILABLE'),
    (UNHEX('2c9ebe4b73925de0858634cc56fb382c'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H01', 45000, 'UNAVAILABLE'),
    (UNHEX('9339d2dae303505c99941562425f4300'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H02', 45000, 'UNAVAILABLE'),
    (UNHEX('f8b43a50d17c5b75b423b1229f1a1957'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H03', 45000, 'AVAILABLE'),
    (UNHEX('8ed0bf6693895055854fdec1d1041f33'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H04', 45000, 'AVAILABLE'),
    (UNHEX('7b6acfe61814533eb7609e25aed403b5'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H05', 45000, 'AVAILABLE'),
    (UNHEX('3a061c70233b529599016e2371358692'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H06', 45000, 'AVAILABLE'),
    (UNHEX('381a8a6b214052abae64afd36c4df2bb'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H07', 45000, 'AVAILABLE'),
    (UNHEX('5eccb9580ad253edb35141c8ed5f0f25'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H08', 45000, 'AVAILABLE'),
    (UNHEX('9718abe543d55e088f537f8fe741e2fe'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H09', 45000, 'AVAILABLE'),
    (UNHEX('7c3aba09fdf45d8b97e036892e4f7edc'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'H10', 45000, 'AVAILABLE'),
    (UNHEX('e1f925c7ad8c57f497003a31a5910122'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J01', 45000, 'AVAILABLE'),
    (UNHEX('75844c47096956c587710e4478685d1b'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J02', 45000, 'AVAILABLE'),
    (UNHEX('03b8b4b781bb59388fdc8a3d76e4bb13'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J03', 45000, 'AVAILABLE'),
    (UNHEX('dd086f8604275de58317597fc432ad5b'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J04', 45000, 'AVAILABLE'),
    (UNHEX('cd023c72cded5f8caaa3ad08eb6a10f9'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J05', 45000, 'AVAILABLE'),
    (UNHEX('5340ec6c76c558468f08f6d8351de639'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J06', 45000, 'AVAILABLE'),
    (UNHEX('d344b093616652b780a6d71d6b42c8c7'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J07', 45000, 'AVAILABLE'),
    (UNHEX('7769c456330c5ca0b90b896636a4c616'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J08', 45000, 'AVAILABLE'),
    (UNHEX('ca455250affc52d4bbfc7cb4a4c522d6'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J09', 45000, 'AVAILABLE'),
    (UNHEX('66d44fb9a4725e86a19e7caeeb0993dc'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'J10', 45000, 'AVAILABLE'),
    (UNHEX('bef70e1570eb5ece8486efe9a021742e'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'K01', 45000, 'AVAILABLE'),
    (UNHEX('2f46f96d15885cffb676ff0d50ba9f19'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'K02', 45000, 'AVAILABLE'),
    (UNHEX('76de8139b38452939927c2ee2cd271df'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'L01', 45000, 'AVAILABLE'),
    (UNHEX('a37a05b6b12952eaa75ea53d782c5eb7'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'L02', 45000, 'AVAILABLE'),
    (UNHEX('74de766145915962bfc958f941b4a727'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'L03', 45000, 'AVAILABLE'),
    (UNHEX('d4829ad3527e5c049ec9b740d1060059'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'L04', 45000, 'AVAILABLE'),
    (UNHEX('04a5ef59c36e52a38f45ec263bc5db1e'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'M01', 100000, 'AVAILABLE'),
    (UNHEX('2cd48c94966a5c08974e03a5f327063e'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'M02', 100000, 'AVAILABLE'),
    (UNHEX('b2368a18dc215a1cb466f97ebb40c3a0'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'N01', 100000, 'UNAVAILABLE'),
    (UNHEX('5eeced21a2295ccbb87a57a48955ccdd'), UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), 'N02', 100000, 'AVAILABLE'),
    (UNHEX('e9cc0f7c40af546ea1e52a60b1f72d91'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A01', 75000, 'AVAILABLE'),
    (UNHEX('a6c5640deb745f2d81bdf795ab58fdce'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A02', 75000, 'AVAILABLE'),
    (UNHEX('599afd14976f5178b0addd28f4ad0585'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A03', 75000, 'AVAILABLE'),
    (UNHEX('980ed097b212575fbd2782b38ff415a8'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A04', 75000, 'AVAILABLE'),
    (UNHEX('17b8b01da5de58138426fd0862d8b6cb'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A05', 75000, 'AVAILABLE'),
    (UNHEX('bdff9d0a9fb65de0aa1db8424299ab5c'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A06', 75000, 'AVAILABLE'),
    (UNHEX('1feed3f2be2f5136bea5aca002ace68d'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A07', 75000, 'AVAILABLE'),
    (UNHEX('9fe3f7b31ede5b11a37969e990767349'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'A08', 75000, 'AVAILABLE'),
    (UNHEX('e879e9386c1359208c0cc048af5a1372'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B01', 75000, 'AVAILABLE'),
    (UNHEX('53bb623b051b5c36b00e34736fde0573'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B02', 75000, 'AVAILABLE'),
    (UNHEX('9fbd8af15e2e5d04a0be2d60b1d58847'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B03', 75000, 'AVAILABLE'),
    (UNHEX('8d72c7a9a6d3533ea9f0730b1a7f9b4c'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B04', 75000, 'AVAILABLE'),
    (UNHEX('4d296e5b3cfb58ec88cde1b9ed6543ba'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B05', 75000, 'AVAILABLE'),
    (UNHEX('360d337f87f850778b17eb7e1f8a4443'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B06', 75000, 'AVAILABLE'),
    (UNHEX('44160be7b0b6594e929e79b30c9e6194'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B07', 75000, 'AVAILABLE'),
    (UNHEX('d430c0e7e0495f77a6628ead12b68bab'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'B08', 75000, 'AVAILABLE'),
    (UNHEX('e4e12b444c585b16b5050f093a13232f'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C01', 75000, 'AVAILABLE'),
    (UNHEX('484276937277576eb65bedbc58f2b78e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C02', 75000, 'AVAILABLE'),
    (UNHEX('76d60ec9dad5565f837264f2a3b91257'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C03', 75000, 'AVAILABLE'),
    (UNHEX('6f19e9df8cfe532b9e229af91841d76a'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C04', 75000, 'AVAILABLE'),
    (UNHEX('af259fbc8d7e59988b83e3cdde6cbd33'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C05', 75000, 'AVAILABLE'),
    (UNHEX('29c2b0e1f0ad5a738cdfd3ff2156bab1'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C06', 75000, 'AVAILABLE'),
    (UNHEX('f4040176cb165feb93d637abf4f97d16'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C07', 75000, 'AVAILABLE'),
    (UNHEX('e7f5d10f886652d0b8e27081c9e16667'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'C08', 75000, 'AVAILABLE'),
    (UNHEX('2ab270e303e257ff83f31130e5575eb4'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D01', 75000, 'AVAILABLE'),
    (UNHEX('8a033862b1b2567591c395854d156e54'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D02', 75000, 'AVAILABLE'),
    (UNHEX('795a5da3a8d4514a84d12a437ee0474b'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D03', 75000, 'AVAILABLE'),
    (UNHEX('bac4cbec4b7b579a8d2442d012896063'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D04', 75000, 'AVAILABLE'),
    (UNHEX('ab68f5c34a8f52ae84e24cdd61a65cb0'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D05', 75000, 'AVAILABLE'),
    (UNHEX('a5d5d048796452baaec174419c48aff7'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D06', 75000, 'AVAILABLE'),
    (UNHEX('457b576ad5c1557a9d5fa46fe42b235f'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D07', 75000, 'AVAILABLE'),
    (UNHEX('82b61392cef25a47b21538a0746b44f1'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'D08', 75000, 'AVAILABLE'),
    (UNHEX('26be6236bc1650c192d2a5dddfef5ff2'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E01', 75000, 'AVAILABLE'),
    (UNHEX('62fbb75c8d665b9baa27290c2a47b60e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E02', 75000, 'AVAILABLE'),
    (UNHEX('11954a85c82c5548849fd0c1a55d08fc'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E03', 75000, 'AVAILABLE'),
    (UNHEX('66f3629f10d05ab181021fd2acd0177a'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E04', 75000, 'AVAILABLE'),
    (UNHEX('0ef85a2eea635ca283606e634b995fcc'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E05', 75000, 'AVAILABLE'),
    (UNHEX('d9c5a4dc4651543192d2dfccdf2ca6c2'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E06', 75000, 'AVAILABLE'),
    (UNHEX('97e07eaf4a8b52e6b404383c38c374cb'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E07', 75000, 'AVAILABLE'),
    (UNHEX('c7f27f2ab7245530b649cd92a2573823'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'E08', 75000, 'AVAILABLE'),
    (UNHEX('a43e9a6dd0a757859ef58ea97093aaf6'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F01', 75000, 'AVAILABLE'),
    (UNHEX('ebe600dd62595d6d8aae8895149be5d2'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F02', 75000, 'AVAILABLE'),
    (UNHEX('7a8b321b81e357c3a33d77aac85d46a5'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F03', 75000, 'AVAILABLE'),
    (UNHEX('c90d3cd348e558aebbbb4df1baf7b457'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F04', 75000, 'AVAILABLE'),
    (UNHEX('80e521e3b153544e88c7c3e9a5d91a1a'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F05', 75000, 'AVAILABLE'),
    (UNHEX('b91bd87419075ae18b5483ea241a6ae5'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F06', 75000, 'AVAILABLE'),
    (UNHEX('7c1104f20ac0520a8445e9203fa1cb36'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F07', 75000, 'AVAILABLE'),
    (UNHEX('f909705be7fa553d9a10206725e51a66'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'F08', 75000, 'AVAILABLE'),
    (UNHEX('80c9ab11128a5bbdb8b86a986fc23c28'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G01', 75000, 'AVAILABLE'),
    (UNHEX('e4d0f2e930925f55bc92c31dfd861275'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G02', 75000, 'AVAILABLE'),
    (UNHEX('826dd3129ebc5635addeb4d109624cd1'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G03', 75000, 'AVAILABLE'),
    (UNHEX('74b14be6414b5b14b383c480d1817f13'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G04', 75000, 'AVAILABLE'),
    (UNHEX('ffbb061e66125e218695fc7063ec517e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G05', 75000, 'AVAILABLE'),
    (UNHEX('0a89e53fb0d253dabfdf5dad8e055b48'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G06', 75000, 'AVAILABLE'),
    (UNHEX('bf4ed52170d25f81bc1fbfaaedf9c448'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G07', 75000, 'AVAILABLE'),
    (UNHEX('e8993c9355a15110af57a53515c1e31e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G08', 75000, 'AVAILABLE'),
    (UNHEX('eda779774c1f58f0bf332097b1c2f306'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G09', 75000, 'AVAILABLE'),
    (UNHEX('20990108836955bbb5cb8a09c23abf85'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'G10', 75000, 'AVAILABLE'),
    (UNHEX('4e45a56605385502ba172a76b17b5d98'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H01', 75000, 'AVAILABLE'),
    (UNHEX('54b2720395aa59989a2920d513de9100'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H02', 75000, 'AVAILABLE'),
    (UNHEX('e4e23d5f4b645f4ea8e178d281709e51'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H03', 75000, 'AVAILABLE'),
    (UNHEX('6be8376bc222544287d2af30a5c834a0'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H04', 75000, 'AVAILABLE'),
    (UNHEX('b7fa78aa16bb50ba966808db03ef8aad'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H05', 75000, 'AVAILABLE'),
    (UNHEX('d92d85e05c285e1d91f3b19b5568de9e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H06', 75000, 'AVAILABLE'),
    (UNHEX('53a3e7c5a95f57238f57eb776c3cd7a1'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H07', 75000, 'AVAILABLE'),
    (UNHEX('3fcb0ddad86e58dc9c8e5d47d245cd1a'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H08', 75000, 'AVAILABLE'),
    (UNHEX('1c8d619fb866512f969c622b24aca2e2'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H09', 75000, 'AVAILABLE'),
    (UNHEX('176fd13ba851507f917e798f9396b577'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'H10', 75000, 'AVAILABLE'),
    (UNHEX('a4af95e053c2585191f73ae27851d979'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J01', 75000, 'AVAILABLE'),
    (UNHEX('0803a322d95750c7b303e87e05ec2857'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J02', 75000, 'AVAILABLE'),
    (UNHEX('2c5759abebf45f81b53163ca4bf149b6'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J03', 75000, 'AVAILABLE'),
    (UNHEX('a8f8804192a754389ec75d607a2b5c8b'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J04', 75000, 'AVAILABLE'),
    (UNHEX('d3316686287b5522a39d40d5555e9588'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J05', 75000, 'AVAILABLE'),
    (UNHEX('38e3f744e3b856a88bc8e6b1d08ea27c'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J06', 75000, 'AVAILABLE'),
    (UNHEX('18faa53e9def554ebe866d95a4eb73cc'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J07', 75000, 'AVAILABLE'),
    (UNHEX('e5661f9f01f15df7a3a50cfdd00a3222'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J08', 75000, 'AVAILABLE'),
    (UNHEX('7696a525cf6859f7b95a2dbcfffdd9b9'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J09', 75000, 'AVAILABLE'),
    (UNHEX('dd135dd262335afc8cd841dce3180490'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'J10', 75000, 'AVAILABLE'),
    (UNHEX('a3d06ce7519e56ecb1b49129d8d64ff4'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'K01', 75000, 'AVAILABLE'),
    (UNHEX('cee538ad54535d2b9fb10b2db63862e3'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'K02', 75000, 'AVAILABLE'),
    (UNHEX('4b038829deb95d0fb2647c056c094867'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'L01', 75000, 'AVAILABLE'),
    (UNHEX('cd677111c4a75df5878c395cb2206881'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'L02', 75000, 'AVAILABLE'),
    (UNHEX('3ac662b714de50d88ba9deac24ecd90f'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'L03', 75000, 'AVAILABLE'),
    (UNHEX('ebb69828e5c457ba969d42dc54aeac09'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'L04', 75000, 'AVAILABLE'),
    (UNHEX('4cd119d9aa025a6bbf0dff422fbb739b'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'M01', 160000, 'AVAILABLE'),
    (UNHEX('9bb5b68ed5235e938675caf09f885615'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'M02', 160000, 'AVAILABLE'),
    (UNHEX('b35f62616a6d5327b815a82b8f874985'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'N01', 160000, 'UNAVAILABLE'),
    (UNHEX('7d87eed5173b55d09e0bdf1861d8ee3e'), UNHEX('34bd6e30676e539d84baf0c05520f832'), 'N02', 160000, 'AVAILABLE');

START TRANSACTION;
SET @cinestar_booking_room = UNHEX('199eebf80cc156bbbc38cec6c7488596');
SET @cinestar_booking_cinema = UNHEX('475ea861a4d55b4a87dc9df6c1853c92');
SET @cinestar_booking_movie = UNHEX('51ff33807d57537398d4015711f374f9');

-- 1. PRECONDITIONS: existing base seed and matching public schedule.
SET @cinestar_booking_room_count = (
    SELECT COUNT(*) FROM rooms r JOIN cinemas c ON c.id = r.cinema_id
    WHERE r.id = @cinestar_booking_room AND r.name = '06' AND r.room_type = 'STANDARD'
      AND c.id = @cinestar_booking_cinema AND r.active = TRUE AND c.active = TRUE
);
INSERT INTO cinestar_booking_guard VALUES ('base_seed_room06_exists_and_active', IF(@cinestar_booking_room_count = 1, 1, 0));
SET @cinestar_booking_show_count = (
    SELECT COUNT(*) FROM showtimes t
    WHERE t.room_id = @cinestar_booking_room AND t.movie_id = @cinestar_booking_movie
      AND (
        (t.id = UNHEX('33a8ee84fe85512ba10f7a70deb792d5') AND t.starts_at = '2026-10-05 11:50:00' AND t.ends_at = '2026-10-05 13:45:00')
        OR (t.id = UNHEX('34bd6e30676e539d84baf0c05520f832') AND t.starts_at = '2026-10-06 11:50:00' AND t.ends_at = '2026-10-06 13:45:00')
      )
);
INSERT INTO cinestar_booking_guard VALUES ('base_seed_two_exact_showtimes_exist', IF(@cinestar_booking_show_count = 2, 1, 0));

-- Do not overwrite manually created layouts or seat deactivations.
SET @cinestar_booking_layout_conflicts = (
    SELECT COUNT(*) FROM seats s
    LEFT JOIN cinestar_booking_layout l ON l.seat_number = s.seat_number
    WHERE s.room_id = @cinestar_booking_room
      AND (l.seed_id IS NULL OR s.row_label <> l.row_label OR s.seat_type <> l.seat_type OR s.active <> TRUE)
);
INSERT INTO cinestar_booking_guard VALUES ('existing_physical_layout_is_compatible', IF(@cinestar_booking_layout_conflicts = 0, 1, 0));

-- Do not overwrite existing price snapshots. Existing local holds/BOOKED states are preserved.
SET @cinestar_booking_price_conflicts = (
    SELECT COUNT(*) FROM show_seats ss
    LEFT JOIN cinestar_booking_prices p ON p.showtime_id = ss.showtime_id AND p.seat_number = ss.seat_number
    LEFT JOIN seats s ON s.id = ss.seat_id
    LEFT JOIN cinestar_booking_layout l ON l.seat_number = ss.seat_number
    WHERE ss.showtime_id IN (UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), UNHEX('34bd6e30676e539d84baf0c05520f832'))
      AND (p.seed_id IS NULL OR l.seed_id IS NULL OR s.id IS NULL
           OR s.room_id <> @cinestar_booking_room OR s.seat_number <> ss.seat_number
           OR ss.seat_type <> l.seat_type OR ss.price <> p.price)
);
INSERT INTO cinestar_booking_guard VALUES ('existing_show_seat_prices_are_compatible', IF(@cinestar_booking_price_conflicts = 0, 1, 0));

-- 2. Add exact physical seats; no UPDATE of seats.
INSERT INTO seats (id, room_id, seat_number, row_label, seat_type, active, version, created_at, updated_at)
SELECT l.seed_id, @cinestar_booking_room, l.seat_number, l.row_label, l.seat_type, TRUE, 0, @cinestar_booking_now, @cinestar_booking_now
FROM cinestar_booking_layout l
LEFT JOIN seats existing ON existing.room_id = @cinestar_booking_room AND existing.seat_number = l.seat_number
WHERE existing.id IS NULL;
SET @cinestar_booking_inserted_seats = ROW_COUNT();

-- 3. Add authoritative adult prices for the TWO observed future showtimes only.
INSERT INTO show_seats (id, showtime_id, seat_id, seat_number, seat_type, price, status, held_by_booking_id, hold_expires_at, version, created_at, updated_at)
SELECT p.seed_id, t.id, s.id, s.seat_number, s.seat_type, p.price, p.snapshot_status, NULL, NULL, 0, @cinestar_booking_now, @cinestar_booking_now
FROM cinestar_booking_prices p
JOIN showtimes t ON t.id = p.showtime_id
JOIN seats s ON s.room_id = t.room_id AND s.seat_number = p.seat_number
LEFT JOIN show_seats existing ON existing.showtime_id = t.id AND (existing.seat_id = s.id OR existing.seat_number = s.seat_number)
WHERE existing.id IS NULL AND t.status IN ('SCHEDULED', 'OPEN_FOR_BOOKING')
  AND t.starts_at > UTC_TIMESTAMP(6) AND s.active = TRUE;
SET @cinestar_booking_inserted_show_seats = ROW_COUNT();

-- 4. Open complete future inventory only. Never reopen CLOSED/CANCELLED/COMPLETED.
UPDATE showtimes t
JOIN rooms r ON r.id = t.room_id
JOIN cinemas c ON c.id = r.cinema_id
JOIN (
    SELECT ss.showtime_id, COUNT(*) AS inventory_count, SUM(ss.status = 'AVAILABLE') AS available_count
    FROM show_seats ss
    WHERE ss.showtime_id IN (UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), UNHEX('34bd6e30676e539d84baf0c05520f832'))
    GROUP BY ss.showtime_id
) inventory ON inventory.showtime_id = t.id
SET t.status = 'OPEN_FOR_BOOKING', t.version = t.version + 1, t.updated_at = UTC_TIMESTAMP(6)
WHERE t.status = 'SCHEDULED' AND t.starts_at > UTC_TIMESTAMP(6)
  AND t.room_id = @cinestar_booking_room AND t.movie_id = @cinestar_booking_movie
  AND r.active = TRUE AND c.active = TRUE
  AND inventory.inventory_count = 88 AND inventory.available_count > 0;
SET @cinestar_booking_opened_showtimes = ROW_COUNT();
COMMIT;

-- 5. Results: added rows in this execution, followed by current inventory.
SELECT @cinestar_booking_inserted_seats AS inserted_seats,
       @cinestar_booking_inserted_show_seats AS inserted_show_seats,
       @cinestar_booking_opened_showtimes AS opened_showtimes;
SELECT BIN_TO_UUID(@cinestar_booking_cinema, 0) AS cinema_id, BIN_TO_UUID(@cinestar_booking_room, 0) AS room_id;
SELECT row_label, seat_type, COUNT(*) AS seat_units FROM seats
WHERE room_id = @cinestar_booking_room GROUP BY row_label, seat_type ORDER BY row_label, seat_type;
SELECT BIN_TO_UUID(t.id, 0) AS showtime_id, t.starts_at AS starts_at_utc,
       DATE_ADD(t.starts_at, INTERVAL 7 HOUR) AS starts_at_cinema_local, t.status AS showtime_status,
       ss.seat_type, ss.price AS adult_price_vnd, ss.status AS seat_status, COUNT(*) AS seat_units
FROM showtimes t LEFT JOIN show_seats ss ON ss.showtime_id = t.id
WHERE t.id IN (UNHEX('33a8ee84fe85512ba10f7a70deb792d5'), UNHEX('34bd6e30676e539d84baf0c05520f832'))
GROUP BY t.id, t.starts_at, t.status, ss.seat_type, ss.price, ss.status
ORDER BY t.starts_at, ss.seat_type, ss.status;

DROP TEMPORARY TABLE cinestar_booking_prices;
DROP TEMPORARY TABLE cinestar_booking_layout;
DROP TEMPORARY TABLE cinestar_booking_guard;
SET SESSION time_zone = @cinestar_booking_previous_time_zone;

-- Manual read-only API checks (Inventory default port 8083; verify your deployment port):
-- GET /api/v1/seats?roomId=199eebf8-0cc1-56bb-bc38-cec6c7488596
-- GET /api/v1/show-seats?showtimeId=33a8ee84-fe85-512b-a10f-7a70deb792d5&availableOnly=false
-- GET /api/v1/show-seats?showtimeId=34bd6e30-676e-539d-84ba-f0c05520f832&availableOnly=false
-- GET /api/v1/showtimes/bookable?cinemaId=475ea861-a4d5-5b4a-87dc-9df6c1853c92&from=2026-10-05T00:00:00%2B07:00&to=2026-10-06T00:00:00%2B07:00
-- For 06/10, change from/to to 2026-10-06/2026-10-07. Offset + must be URL-encoded as %2B.
