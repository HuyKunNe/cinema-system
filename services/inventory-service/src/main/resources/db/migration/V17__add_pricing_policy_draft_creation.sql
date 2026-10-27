ALTER TABLE cinema_pricing_policy_versions
    ADD COLUMN draft_source_version_id BINARY(16) NULL,
    ADD COLUMN draft_client_request_id BINARY(16) NULL,

    ADD KEY idx_pricing_version_draft_source (
        draft_source_version_id
    ),

    ADD CONSTRAINT uk_pricing_version_draft_request
        UNIQUE (policy_id, draft_client_request_id),

    ADD CONSTRAINT fk_pricing_version_draft_source
        FOREIGN KEY (draft_source_version_id)
        REFERENCES cinema_pricing_policy_versions (id),

    ADD CONSTRAINT chk_pricing_version_draft_origin
        CHECK (
            (
                draft_source_version_id IS NULL
                AND draft_client_request_id IS NULL
            )
            OR
            (
                draft_source_version_id IS NOT NULL
                AND draft_client_request_id IS NOT NULL
            )
        ),

    ADD CONSTRAINT chk_pricing_version_draft_source
        CHECK (
            draft_source_version_id IS NULL
            OR draft_source_version_id <> id
        );
