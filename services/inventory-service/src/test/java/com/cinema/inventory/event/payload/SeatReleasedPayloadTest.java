package com.cinema.inventory.event.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.inventory.event.InventoryEventContract;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class SeatReleasedPayloadTest {

    private static final OffsetDateTime RELEASED_AT = OffsetDateTime.parse("2026-09-15T10:01:00Z");

    @Test
    void shouldCreateImmutableReleasedSeatSnapshot() {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID firstSeatId = UuidGenerator.next();

        UUID secondSeatId = UuidGenerator.next();

        List<UUID> sourceSeatIds = new ArrayList<>(List.of(firstSeatId, secondSeatId));

        SeatReleasedPayload payload =
                new SeatReleasedPayload(
                        bookingId,
                        showtimeId,
                        sourceSeatIds,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        RELEASED_AT);

        sourceSeatIds.add(UuidGenerator.next());

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.releasedSeatIds()).containsExactly(firstSeatId, secondSeatId);

        assertThat(payload.reason())
                .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

        assertThat(payload.releasedAt()).isEqualTo(RELEASED_AT);
    }

    @Test
    void shouldPreventMutationThroughReleasedSeatIdList() {

        SeatReleasedPayload payload =
                new SeatReleasedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        List.of(UuidGenerator.next()),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        RELEASED_AT);

        assertThatThrownBy(() -> payload.releasedSeatIds().add(UuidGenerator.next()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullReleasedSeatListShouldBecomeImmutableEmptyList() {

        SeatReleasedPayload payload =
                new SeatReleasedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        null,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        RELEASED_AT);

        assertThat(payload.releasedSeatIds()).isEmpty();

        assertThatThrownBy(() -> payload.releasedSeatIds().add(UuidGenerator.next()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
