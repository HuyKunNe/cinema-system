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
