CREATE TABLE room_screening_formats (
    room_id BINARY(16) NOT NULL,
    screening_format VARCHAR(40) NOT NULL,

    CONSTRAINT pk_room_screening_formats
        PRIMARY KEY (room_id, screening_format),

    CONSTRAINT fk_room_screening_formats_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_room_screening_formats_value
        CHECK (
            screening_format IN (
                'STANDARD_2D',
                'STANDARD_3D',
                'PREMIUM_CINEMA_2D',
                'PREMIUM_CINEMA_3D',
                'FOUR_DX_2D',
                'FOUR_DX_3D',
                'IMAX_2D',
                'IMAX_3D'
            )
        )
);
