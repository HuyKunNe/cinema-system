-- Retain draft rows and dependencies to preserve revision and idempotency history.
ALTER TABLE room_layouts
    ADD COLUMN deleted BIT(1) NOT NULL DEFAULT b'0';

ALTER TABLE cinema_pricing_policy_versions
    ADD COLUMN deleted BIT(1) NOT NULL DEFAULT b'0';
