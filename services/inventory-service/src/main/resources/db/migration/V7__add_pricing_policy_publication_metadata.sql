ALTER TABLE cinema_pricing_policies
    ADD COLUMN published_by BINARY(16) NULL,
    ADD COLUMN publication_confirmation_reference VARCHAR(500) NULL,
    ADD CONSTRAINT chk_pricing_policy_publication_metadata
        CHECK (
            (
                published_by IS NULL
                AND publication_confirmation_reference IS NULL
            )
            OR
            (
                status = 'PUBLISHED'
                AND published_by IS NOT NULL
                AND publication_confirmation_reference IS NOT NULL
            )
        );
