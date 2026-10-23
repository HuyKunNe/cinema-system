ALTER TABLE room_layouts
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_room_layouts_content_revision
        CHECK (content_revision >= 0);
