package com.cinema.booking.event.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class SeatReservationRequestedPayloadTest {

    @Test
    void seatCollectionShouldBeImmutableSnapshot() {

        List<RequestedSeatPayload> source =
                new ArrayList<>(List.of(new RequestedSeatPayload("H7")));

        SeatReservationRequestedPayload payload =
                new SeatReservationRequestedPayload(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        source,
                        OffsetDateTime.parse("2026-08-21T10:00:00Z"),
                        OffsetDateTime.parse("2026-08-21T10:10:00Z"));

        source.add(new RequestedSeatPayload("H8"));

        assertThat(payload.seats()).containsExactly(new RequestedSeatPayload("H7"));

        assertThrows(
                UnsupportedOperationException.class,
                () -> payload.seats().add(new RequestedSeatPayload("H9")));
    }
}
