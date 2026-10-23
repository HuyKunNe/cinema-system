ALTER TABLE showtimes
    ADD COLUMN room_layout_id BINARY(16) NULL,
    ADD INDEX idx_showtimes_room_layout (room_layout_id),
    ADD CONSTRAINT fk_showtimes_room_layout
        FOREIGN KEY (room_layout_id)
        REFERENCES room_layouts (id);
