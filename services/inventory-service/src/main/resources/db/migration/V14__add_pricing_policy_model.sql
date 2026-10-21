ALTER TABLE cinema_pricing_policies
    ADD COLUMN pricing_model VARCHAR(30)
        NOT NULL DEFAULT 'BASE_RULES_V1',
    ADD CONSTRAINT chk_cinema_pricing_policy_model
        CHECK (
            pricing_model IN (
                'BASE_RULES_V1',
                'FARE_MATRIX_V2'
            )
        );
