ALTER TABLE booking_tickets
    ADD COLUMN issuance_source VARCHAR(30) NULL,
    ADD COLUMN issued_by BINARY(16) NULL,
    ADD CONSTRAINT chk_booking_tickets_issuance_audit CHECK (
        (issuance_source IS NULL AND issued_by IS NULL)
        OR (issuance_source IS NOT NULL AND issuance_source = 'BOOKING_CONFIRMED_EVENT' AND issued_by IS NULL)
        OR (issuance_source IS NOT NULL AND issuance_source = 'ADMIN_RECOVERY' AND issued_by IS NOT NULL)
    );
-- Existing tickets keep unknown provenance; do not fabricate an actor/source.
