CREATE INDEX idx_oauth2_authorization_principal
    ON oauth2_authorization (principal_name);

CREATE INDEX idx_oauth2_authorization_client
    ON oauth2_authorization (registered_client_id);
