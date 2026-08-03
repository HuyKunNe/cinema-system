ALTER TABLE show_seats
    ADD CONSTRAINT uk_show_seat_showtime_seat
    UNIQUE (showtime_id, seat_id);
