ALTER TABLE cinema_pricing_policy_versions
    ADD COLUMN published_at DATETIME(6) NULL,
    ADD COLUMN published_by VARCHAR(255) NULL;
