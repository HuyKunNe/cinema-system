CREATE TABLE showtime_batch_requests (
    client_request_id BINARY(16) NOT NULL,
    request_hash CHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    response_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (client_request_id)
) ENGINE = InnoDB;
