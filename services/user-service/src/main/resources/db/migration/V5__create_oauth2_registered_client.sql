CREATE TABLE oauth2_registered_client
(
    id VARCHAR(100)
    CHARACTER SET ascii
    COLLATE ascii_bin
    NOT NULL,

    client_id VARCHAR(100)
    CHARACTER SET ascii
    COLLATE ascii_bin
    NOT NULL,

    client_secret VARCHAR(200)
    CHARACTER SET ascii
    COLLATE ascii_bin
    NULL,
    client_id_issued_at           TIMESTAMP(6)  NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),
    client_secret_expires_at      TIMESTAMP(6)  NULL,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000) NULL,
    post_logout_redirect_uris     VARCHAR(1000) NULL,
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL,

    CONSTRAINT pk_oauth2_registered_client
        PRIMARY KEY (id),

    CONSTRAINT uk_oauth2_registered_client_client_id
        UNIQUE (client_id),

    CONSTRAINT chk_oauth2_registered_client_id
        CHECK (CHAR_LENGTH(TRIM(id)) > 0),

    CONSTRAINT chk_oauth2_registered_client_client_id
        CHECK (CHAR_LENGTH(TRIM(client_id)) > 0),

    CONSTRAINT chk_oauth2_registered_client_name
        CHECK (CHAR_LENGTH(TRIM(client_name)) > 0)
) ENGINE = InnoDB;
