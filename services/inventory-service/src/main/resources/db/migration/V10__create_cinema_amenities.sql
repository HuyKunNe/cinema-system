CREATE TABLE cinema_amenities (
    cinema_id BINARY(16) NOT NULL,
    amenity VARCHAR(100)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL,

    CONSTRAINT pk_cinema_amenities
        PRIMARY KEY (cinema_id, amenity),

    CONSTRAINT fk_cinema_amenities_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id)
);
