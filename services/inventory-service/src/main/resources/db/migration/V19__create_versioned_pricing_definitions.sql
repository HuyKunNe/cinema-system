CREATE TABLE pricing_time_bands (
    id BINARY(16) NOT NULL,
    policy_version_id BINARY(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_minute INT NOT NULL,
    end_minute INT NOT NULL,
    sort_order INT NOT NULL,
    legacy_code VARCHAR(20)
        CHARACTER SET ascii COLLATE ascii_bin NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_pricing_time_bands_owner
        UNIQUE (policy_version_id, id),

    CONSTRAINT uk_pricing_time_bands_legacy
        UNIQUE (policy_version_id, legacy_code),

    CONSTRAINT fk_pricing_time_bands_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT chk_pricing_time_bands_start
        CHECK (start_minute >= 0 AND start_minute < 1440),

    CONSTRAINT chk_pricing_time_bands_end
        CHECK (end_minute >= 0 AND end_minute <= 1440),

    CONSTRAINT chk_pricing_time_bands_range
        CHECK (start_minute <> end_minute),

    CONSTRAINT chk_pricing_time_bands_order
        CHECK (sort_order >= 0),

    CONSTRAINT chk_pricing_time_bands_legacy
        CHECK (
            legacy_code IS NULL
            OR legacy_code IN ('BEFORE_17', 'FROM_17')
        )
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE pricing_ticket_groups (
    id BINARY(16) NOT NULL,
    policy_version_id BINARY(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    eligibility_description VARCHAR(1000) NULL,
    default_group BOOLEAN NOT NULL,
    sort_order INT NOT NULL,
    legacy_category VARCHAR(30)
        CHARACTER SET ascii COLLATE ascii_bin NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_pricing_ticket_groups_owner
        UNIQUE (policy_version_id, id),

    CONSTRAINT uk_pricing_ticket_groups_legacy
        UNIQUE (policy_version_id, legacy_category),

    CONSTRAINT fk_pricing_ticket_groups_version
        FOREIGN KEY (policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    CONSTRAINT chk_pricing_ticket_groups_order
        CHECK (sort_order >= 0),

    CONSTRAINT chk_pricing_ticket_groups_default
        CHECK (default_group IN (0, 1)),

    CONSTRAINT chk_pricing_ticket_groups_default_condition
        CHECK (
            default_group = 0
            OR eligibility_description IS NULL
        ),

    CONSTRAINT chk_pricing_ticket_groups_legacy
        CHECK (
            legacy_category IS NULL
            OR legacy_category IN (
                'ADULT', 'SENIOR_55_PLUS', 'STUDENT_U22', 'CHILD'
            )
        )
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO pricing_time_bands (
    id,
    policy_version_id,
    name,
    start_minute,
    end_minute,
    sort_order,
    legacy_code,
    created_at,
    updated_at
)
SELECT
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
            SUBSTRING(seed.random_hex, 1, 3),
            SUBSTRING(
                '89AB',
                MOD(
                    CONV(SUBSTRING(seed.random_hex, 4, 1), 16, 10),
                    4
                ) + 1,
                1
            ),
            SUBSTRING(seed.random_hex, 5, 15)
        )
    ),
    seed.policy_version_id,
    seed.name,
    seed.start_minute,
    seed.end_minute,
    seed.sort_order,
    seed.legacy_code,
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
FROM (
    SELECT
        revision.id AS policy_version_id,
        template.name,
        template.start_minute,
        template.end_minute,
        template.sort_order,
        template.legacy_code,
        HEX(RANDOM_BYTES(10)) AS random_hex
    FROM cinema_pricing_policy_versions revision
    CROSS JOIN (
        SELECT
            'Trước 17:00' AS name,
            0 AS start_minute,
            1020 AS end_minute,
            0 AS sort_order,
            'BEFORE_17' AS legacy_code
        UNION ALL
        SELECT
            'Từ 17:00',
            1020,
            1440,
            1,
            'FROM_17'
    ) template
) seed;

INSERT INTO pricing_ticket_groups (
    id,
    policy_version_id,
    name,
    eligibility_description,
    default_group,
    sort_order,
    legacy_category,
    created_at,
    updated_at
)
SELECT
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
            SUBSTRING(seed.random_hex, 1, 3),
            SUBSTRING(
                '89AB',
                MOD(
                    CONV(SUBSTRING(seed.random_hex, 4, 1), 16, 10),
                    4
                ) + 1,
                1
            ),
            SUBSTRING(seed.random_hex, 5, 15)
        )
    ),
    seed.policy_version_id,
    CASE seed.ticket_category
        WHEN 'ADULT' THEN 'Người lớn'
        WHEN 'SENIOR_55_PLUS' THEN 'Người cao tuổi từ 55 tuổi'
        WHEN 'STUDENT_U22' THEN 'Học sinh, sinh viên dưới 22 tuổi'
        WHEN 'CHILD' THEN 'Trẻ em'
    END,
    NULL,
    seed.ticket_category = 'ADULT',
    CASE seed.ticket_category
        WHEN 'ADULT' THEN 0
        WHEN 'SENIOR_55_PLUS' THEN 1
        WHEN 'STUDENT_U22' THEN 2
        WHEN 'CHILD' THEN 3
    END,
    seed.ticket_category,
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
FROM (
    SELECT
        source_groups.policy_version_id,
        source_groups.ticket_category,
        HEX(RANDOM_BYTES(10)) AS random_hex
    FROM (
        SELECT policy_version_id, ticket_category
        FROM pricing_fare_categories

        UNION

        SELECT id, 'ADULT'
        FROM cinema_pricing_policy_versions
    ) source_groups
) seed;

ALTER TABLE pricing_fare_cells
    ADD COLUMN time_band_id BINARY(16) NULL,
    ADD COLUMN ticket_group_id BINARY(16) NULL;

UPDATE pricing_fare_cells cell
JOIN pricing_time_bands time_band
    ON time_band.policy_version_id = cell.policy_version_id
   AND time_band.legacy_code = cell.time_band
JOIN pricing_ticket_groups ticket_group
    ON ticket_group.policy_version_id = cell.policy_version_id
   AND ticket_group.legacy_category = cell.ticket_category
SET
    cell.time_band_id = time_band.id,
    cell.ticket_group_id = ticket_group.id;

ALTER TABLE pricing_fare_cells
    MODIFY COLUMN time_band_id BINARY(16) NOT NULL,
    MODIFY COLUMN ticket_group_id BINARY(16) NOT NULL,

    ADD CONSTRAINT uk_pricing_fare_cell_definitions
        UNIQUE (
            policy_version_id,
            experience_type,
            content_format,
            time_band_id,
            ticket_group_id,
            seat_type
        ),

    ADD CONSTRAINT fk_pricing_fare_cells_time_band_definition
        FOREIGN KEY (policy_version_id, time_band_id)
        REFERENCES pricing_time_bands (policy_version_id, id),

    ADD CONSTRAINT fk_pricing_fare_cells_ticket_group
        FOREIGN KEY (policy_version_id, ticket_group_id)
        REFERENCES pricing_ticket_groups (policy_version_id, id);
