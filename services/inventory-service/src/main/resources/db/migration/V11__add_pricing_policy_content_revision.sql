ALTER TABLE cinema_pricing_policies
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_pricing_policy_content_revision
        CHECK (content_revision >= 0);
