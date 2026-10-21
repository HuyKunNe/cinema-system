-- Consolidated schema for fresh Inventory databases.

-- Source: V1__create_inventory_schema.sql
CREATE TABLE cinemas (
    id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_cinemas PRIMARY KEY (id)
);

CREATE INDEX idx_cinemas_city_active
    ON cinemas (city, active);

CREATE TABLE rooms (
    id BINARY(16) NOT NULL,
    cinema_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_rooms PRIMARY KEY (id),

    CONSTRAINT fk_rooms_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id),

    CONSTRAINT uk_rooms_cinema_name
        UNIQUE (cinema_id, name)
);

CREATE INDEX idx_rooms_cinema_active
    ON rooms (cinema_id, active);

CREATE TABLE seats (
    id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_seats PRIMARY KEY (id),

    CONSTRAINT fk_seats_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id),

    CONSTRAINT uk_seats_room_number
        UNIQUE (room_id, seat_number)
);

CREATE INDEX idx_seats_room_active
    ON seats (room_id, active);

CREATE TABLE showtimes (
    id BINARY(16) NOT NULL,
    movie_id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_showtimes PRIMARY KEY (id),

    CONSTRAINT fk_showtimes_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id),

    CONSTRAINT uk_showtimes_room_start
        UNIQUE (room_id, starts_at),

    CONSTRAINT chk_showtimes_time
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_showtimes_movie_start
    ON showtimes (movie_id, starts_at);

CREATE INDEX idx_showtimes_room_status_start
    ON showtimes (room_id, status, starts_at);

CREATE TABLE show_seats (
    id BINARY(16) NOT NULL,
    showtime_id BINARY(16) NOT NULL,
    seat_id BINARY(16) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    seat_type VARCHAR(50) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    held_by_booking_id BINARY(16) NULL,
    hold_expires_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_show_seats PRIMARY KEY (id),

    CONSTRAINT fk_show_seats_showtime
        FOREIGN KEY (showtime_id)
        REFERENCES showtimes (id),

    CONSTRAINT fk_show_seats_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats (id),

    CONSTRAINT uk_show_seats_showtime_seat
        UNIQUE (showtime_id, seat_id),

    CONSTRAINT uk_show_seats_showtime_number
        UNIQUE (showtime_id, seat_number),

    CONSTRAINT chk_show_seats_price
        CHECK (price >= 0)
);

CREATE INDEX idx_show_seats_showtime_status
    ON show_seats (showtime_id, status);

CREATE INDEX idx_show_seats_booking
    ON show_seats (held_by_booking_id);

CREATE INDEX idx_show_seats_expired_hold
    ON show_seats (status, hold_expires_at);

-- Source: V2__create_event_infrastructure.sql
CREATE TABLE processed_events (
    id BINARY(16) NOT NULL,
    event_id BINARY(16) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    processed_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_processed_events
        PRIMARY KEY (id),

    CONSTRAINT uk_processed_events_event_consumer
        UNIQUE (
            event_id,
            consumer_name
        )
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events (processed_at);

CREATE INDEX idx_processed_events_type
    ON processed_events (
        event_type,
        processed_at
    );

CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    correlation_id BINARY(16) NULL,
    causation_id BINARY(16) NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    last_error VARCHAR(2000) NULL,
    processing_owner VARCHAR(150) NULL,
    processing_started_at DATETIME(6) NULL,
    processing_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,

    CONSTRAINT pk_outbox_events
        PRIMARY KEY (id),

    CONSTRAINT chk_outbox_events_retry_count
        CHECK (retry_count >= 0),

    CONSTRAINT chk_outbox_events_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        )
);

CREATE INDEX idx_outbox_events_claim
    ON outbox_events (
        status,
        next_attempt_at,
        processing_expires_at,
        created_at
    );

CREATE INDEX idx_outbox_events_processing_owner
    ON outbox_events (
        processing_owner,
        status
    );

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (
        aggregate_type,
        aggregate_id
    );

CREATE INDEX idx_outbox_events_correlation
    ON outbox_events (correlation_id);
