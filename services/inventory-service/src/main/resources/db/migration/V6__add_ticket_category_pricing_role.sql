ALTER TABLE pricing_ticket_categories
    ADD COLUMN pricing_role VARCHAR(30)
        NOT NULL DEFAULT 'UNCONFIRMED',
    ADD CONSTRAINT chk_pricing_ticket_categories_role
        CHECK (
            pricing_role IN (
                'UNCONFIRMED',
                'REGULAR',
                'CATEGORY_DISCOUNT',
                'C_MONDAY',
                'C_MEMBER',
                'C_TEN'
            )
        );
