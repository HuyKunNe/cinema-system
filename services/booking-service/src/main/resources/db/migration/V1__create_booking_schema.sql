CREATE TABLE bookings (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    showtime_id BINARY(16) NOT NULL,
    client_request_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(19, 2) NULL,
    currency VARCHAR(3) NULL,
    expires_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    rejection_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_bookings
        PRIMARY KEY (id),

    CONSTRAINT uk_bookings_user_client_request
        UNIQUE (
            user_id,
            client_request_id
        ),

    CONSTRAINT chk_bookings_total_non_negative
        CHECK (
            total_amount IS NULL
            OR total_amount >= 0
        ),

    CONSTRAINT chk_bookings_money_snapshot
        CHECK (
            (
                total_amount IS NULL
                AND currency IS NULL
            )
            OR
            (
                total_amount IS NOT NULL
                AND currency IS NOT NULL
                AND CHAR_LENGTH(currency) = 3
            )
        )
);

CREATE INDEX idx_bookings_user_created
    ON bookings (
        user_id,
        created_at
    );

CREATE INDEX idx_bookings_status_expiration
    ON bookings (
        status,
        expires_at
    );

CREATE TABLE booking_seats (
    id BINARY(16) NOT NULL,
    booking_id BINARY(16) NOT NULL,
    inventory_seat_id BINARY(16) NULL,
    showtime_id BINARY(16) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    seat_type VARCHAR(50) NULL,
    price DECIMAL(19, 2) NULL,
    created_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_booking_seats
        PRIMARY KEY (id),

    CONSTRAINT fk_booking_seats_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings (id),

    CONSTRAINT uk_booking_seats_booking_seat
        UNIQUE (
            booking_id,
            showtime_id,
            seat_number
        ),

    CONSTRAINT chk_booking_seats_price_non_negative
        CHECK (
            price IS NULL
            OR price >= 0
        ),

    CONSTRAINT chk_booking_seats_snapshot
        CHECK (
            (
                inventory_seat_id IS NULL
                AND seat_type IS NULL
                AND price IS NULL
            )
            OR
            (
                inventory_seat_id IS NOT NULL
                AND seat_type IS NOT NULL
                AND price IS NOT NULL
            )
        )
);

CREATE INDEX idx_booking_seats_booking
    ON booking_seats (booking_id);
