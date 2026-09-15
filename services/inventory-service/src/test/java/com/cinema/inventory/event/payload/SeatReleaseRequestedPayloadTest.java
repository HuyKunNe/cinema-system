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

class SeatReleaseRequestedPayloadTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    @Test
    void shouldCreateImmutableSeatIdSnapshot() {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID firstSeatId = UuidGenerator.next();

        UUID secondSeatId = UuidGenerator.next();

        List<UUID> sourceSeatIds = new ArrayList<>(List.of(firstSeatId, secondSeatId));

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        showtimeId,
                        sourceSeatIds,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        sourceSeatIds.add(UuidGenerator.next());

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.seatIds()).containsExactly(firstSeatId, secondSeatId);

        assertThat(payload.reason())
                .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

        assertThat(payload.requestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void shouldPreventMutationThroughSeatIdList() {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        List.of(UuidGenerator.next()),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        assertThatThrownBy(() -> payload.seatIds().add(UuidGenerator.next()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullSeatIdListShouldBecomeImmutableEmptyList() {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        null,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        assertThat(payload.seatIds()).isEmpty();

        assertThatThrownBy(() -> payload.seatIds().add(UuidGenerator.next()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
