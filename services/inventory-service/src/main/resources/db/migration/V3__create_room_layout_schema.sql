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
