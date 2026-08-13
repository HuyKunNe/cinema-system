ALTER TABLE oauth2_registered_client
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_oauth2_registered_client_active
    ON oauth2_registered_client (active);
