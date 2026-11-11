-- Expand lifecycle values only. Existing published versions and showtime references
-- remain unchanged; the next successful publication retires prior published versions.
ALTER TABLE room_layouts
    DROP CHECK chk_room_layouts_publication,
    DROP CHECK chk_room_layouts_status,
    ADD CONSTRAINT chk_room_layouts_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    ADD CONSTRAINT chk_room_layouts_publication
        CHECK ((status = 'DRAFT' AND published_at IS NULL)
            OR (status IN ('PUBLISHED', 'RETIRED') AND published_at IS NOT NULL));
