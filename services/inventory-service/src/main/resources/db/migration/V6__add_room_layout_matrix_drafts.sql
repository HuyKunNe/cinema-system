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
