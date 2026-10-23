CREATE TABLE room_supported_content_formats (
    room_id BINARY(16) NOT NULL,
    content_format VARCHAR(20) NOT NULL,

    CONSTRAINT pk_room_supported_content_formats
        PRIMARY KEY (room_id, content_format),

    CONSTRAINT fk_room_supported_content_formats_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id),

    CONSTRAINT chk_room_supported_content_format
        CHECK (content_format IN ('TWO_D', 'THREE_D'))
);
