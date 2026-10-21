-- Consolidated schema for fresh Inventory databases.

-- Source: V3__create_room_layout_schema.sql
CREATE TABLE room_layouts (
    id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    layout_version BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    canvas_width DECIMAL(12, 3) NOT NULL,
    canvas_height DECIMAL(12, 3) NOT NULL,
    published_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_room_layouts
        PRIMARY KEY (id),

    CONSTRAINT fk_room_layouts_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id),

    CONSTRAINT uk_room_layouts_room_version
        UNIQUE (room_id, layout_version),

    CONSTRAINT chk_room_layouts_layout_version
        CHECK (layout_version > 0),

    CONSTRAINT chk_room_layouts_status
        CHECK (status IN ('DRAFT', 'PUBLISHED')),

    CONSTRAINT chk_room_layouts_canvas
        CHECK (canvas_width > 0 AND canvas_height > 0),

    CONSTRAINT chk_room_layouts_publication
        CHECK (
            (status = 'DRAFT' AND published_at IS NULL)
            OR
            (status = 'PUBLISHED' AND published_at IS NOT NULL)
        )
);

CREATE INDEX idx_room_layouts_room_status
    ON room_layouts (room_id, status);

CREATE TABLE room_layout_seats (
    id BINARY(16) NOT NULL,
    layout_id BINARY(16) NOT NULL,
    seat_id BINARY(16) NOT NULL,
    seat_number_snapshot VARCHAR(20) NOT NULL,
    row_label_snapshot VARCHAR(10) NOT NULL,
    seat_type_snapshot VARCHAR(50) NOT NULL,
    x DECIMAL(12, 3) NOT NULL,
    y DECIMAL(12, 3) NOT NULL,
    width DECIMAL(12, 3) NOT NULL,
    height DECIMAL(12, 3) NOT NULL,
    rotation_degrees DECIMAL(9, 3) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_room_layout_seats
        PRIMARY KEY (id),

    CONSTRAINT fk_room_layout_seats_layout
        FOREIGN KEY (layout_id)
        REFERENCES room_layouts (id),

    CONSTRAINT fk_room_layout_seats_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats (id),

    CONSTRAINT uk_room_layout_seats_layout_seat
        UNIQUE (layout_id, seat_id),

    CONSTRAINT chk_room_layout_seats_type
        CHECK (
            seat_type_snapshot IN (
                'STANDARD',
                'VIP',
                'COUPLE',
                'ACCESSIBLE'
            )
        ),

    CONSTRAINT chk_room_layout_seats_position
        CHECK (x >= 0 AND y >= 0),

    CONSTRAINT chk_room_layout_seats_size
        CHECK (width > 0 AND height > 0)
);

CREATE TABLE room_layout_elements (
    id BINARY(16) NOT NULL,
    layout_id BINARY(16) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    label VARCHAR(150) NULL,
    x DECIMAL(12, 3) NOT NULL,
    y DECIMAL(12, 3) NOT NULL,
    width DECIMAL(12, 3) NOT NULL,
    height DECIMAL(12, 3) NOT NULL,
    rotation_degrees DECIMAL(9, 3) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_room_layout_elements
        PRIMARY KEY (id),

    CONSTRAINT fk_room_layout_elements_layout
        FOREIGN KEY (layout_id)
        REFERENCES room_layouts (id),

    CONSTRAINT chk_room_layout_elements_kind
        CHECK (kind IN ('SCREEN', 'AISLE', 'EXIT')),

    CONSTRAINT chk_room_layout_elements_position
        CHECK (x >= 0 AND y >= 0),

    CONSTRAINT chk_room_layout_elements_size
        CHECK (width > 0 AND height > 0)
);

CREATE INDEX idx_room_layout_elements_layout
    ON room_layout_elements (layout_id);

-- Source: V4__add_room_layout_content_revision.sql
ALTER TABLE room_layouts
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_room_layouts_content_revision
        CHECK (content_revision >= 0);

-- Source: V5__add_showtime_room_layout.sql
ALTER TABLE showtimes
    ADD COLUMN room_layout_id BINARY(16) NULL,
    ADD INDEX idx_showtimes_room_layout (room_layout_id),
    ADD CONSTRAINT fk_showtimes_room_layout
        FOREIGN KEY (room_layout_id)
        REFERENCES room_layouts (id);

-- Source: V6__add_room_layout_matrix_drafts.sql
ALTER TABLE room_layouts
    ADD COLUMN row_count INT NULL,
    ADD COLUMN column_count INT NULL,
    ADD CONSTRAINT chk_room_layouts_matrix_dimensions
        CHECK (
            (row_count IS NULL AND column_count IS NULL)
            OR (
                row_count IS NOT NULL
                AND column_count IS NOT NULL
                AND row_count BETWEEN 1 AND 100
                AND column_count BETWEEN 1 AND 100
            )
        );

CREATE TABLE room_layout_matrix_cells (
    id BINARY(16) NOT NULL,
    layout_id BINARY(16) NOT NULL,
    seat_id BINARY(16) NULL,
    row_index INT NOT NULL,
    column_index INT NOT NULL,
    seat_number_in_row INT NOT NULL,
    seat_type VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_room_layout_matrix_cells
        PRIMARY KEY (id),

    CONSTRAINT fk_room_layout_matrix_cells_layout
        FOREIGN KEY (layout_id)
        REFERENCES room_layouts (id),

    CONSTRAINT fk_room_layout_matrix_cells_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats (id),

    CONSTRAINT uk_room_layout_matrix_cells_position
        UNIQUE (layout_id, row_index, column_index),

    CONSTRAINT uk_room_layout_matrix_cells_number
        UNIQUE (layout_id, row_index, seat_number_in_row),

    CONSTRAINT uk_room_layout_matrix_cells_seat
        UNIQUE (layout_id, seat_id),

    CONSTRAINT chk_room_layout_matrix_cells_indices
        CHECK (
            row_index BETWEEN 0 AND 99
            AND column_index BETWEEN 0 AND 99
        ),

    CONSTRAINT chk_room_layout_matrix_cells_number
        CHECK (seat_number_in_row BETWEEN 1 AND 100),

    CONSTRAINT chk_room_layout_matrix_cells_type
        CHECK (
            seat_type IN ('STANDARD', 'VIP', 'COUPLE', 'ACCESSIBLE')
        )
);

-- Source: V7__support_layout_managed_seats.sql
ALTER TABLE seats
    ADD COLUMN layout_managed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN legacy_seat_number VARCHAR(20)
        GENERATED ALWAYS AS (
            CASE
                WHEN layout_managed = FALSE THEN seat_number
                ELSE NULL
            END
        ) STORED,
    DROP INDEX uk_seats_room_number,
    ADD CONSTRAINT uk_seats_room_legacy_number
        UNIQUE (room_id, legacy_seat_number);
