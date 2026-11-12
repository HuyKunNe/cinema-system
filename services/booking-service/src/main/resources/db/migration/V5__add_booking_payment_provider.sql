ALTER TABLE bookings
    ADD COLUMN payment_provider VARCHAR(20) NULL AFTER request_fingerprint;
