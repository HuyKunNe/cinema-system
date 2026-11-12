ALTER TABLE payment_transactions
    ADD COLUMN checkout_uri VARCHAR(2048) NULL AFTER provider_reference;
