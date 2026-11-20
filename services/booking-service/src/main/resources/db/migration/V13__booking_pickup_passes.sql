CREATE TABLE booking_pickup_passes (
    id BINARY(16) NOT NULL,
    booking_id BINARY(16) NOT NULL,
    qr_token VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_booking_pickup_booking UNIQUE (booking_id),
    CONSTRAINT uk_booking_pickup_token UNIQUE (qr_token),
    CONSTRAINT fk_booking_pickup_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_booking_pickup_token CHECK (REGEXP_LIKE(qr_token, '^[A-Za-z0-9_-]{43}$')),
    INDEX idx_booking_pickup_expiry (expires_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
