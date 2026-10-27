ALTER TABLE showtimes
    ADD COLUMN pricing_policy_version_id BINARY(16) NULL,

    ADD KEY idx_showtimes_pricing_policy_version (
        pricing_policy_version_id
    ),

    ADD CONSTRAINT fk_showtimes_pricing_policy_version
        FOREIGN KEY (pricing_policy_version_id)
        REFERENCES cinema_pricing_policy_versions (id);
