package com.cinema.inventory.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class DefaultSeatReleasedOutboxFactoryTest {

    private static final OffsetDateTime RELEASED_AT = OffsetDateTime.parse("2026-09-15T10:01:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultSeatReleasedOutboxFactory factory =
            new DefaultSeatReleasedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalSeatReleasedOutboxEvent() throws Exception {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID causationId = UuidGenerator.next();

        Showtime showtime = createShowtime();

        ShowSeat h8 = createShowSeat(showtime, "H8", SeatType.VIP, new BigDecimal("120000.00"));

        ShowSeat h7 = createShowSeat(showtime, "H7", SeatType.STANDARD, new BigDecimal("90000.00"));

        OutboxEventEntity event =
                factory.create(
                        bookingId,
                        showtimeId,
                        List.of(h8, h7),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        RELEASED_AT,
                        correlationId,
                        causationId);

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getEventType()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(event.getEventVersion()).isEqualTo(InventoryEventContract.SEAT_RELEASED_VERSION);

        assertThat(event.getTopic()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getOccurredAt()).isEqualTo(RELEASED_AT);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(causationId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(RELEASED_AT);

        assertThat(event.getCreatedAt()).isEqualTo(RELEASED_AT);

        assertThat(event.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.get("reason").asText())
                .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

        assertThat(OffsetDateTime.parse(payload.get("releasedAt").asText())).isEqualTo(RELEASED_AT);

        List<UUID> expectedSeatIds =
                List.of(h8.getId(), h7.getId()).stream().sorted(Comparator.naturalOrder()).toList();

        assertThat(payload.get("releasedSeatIds"))
                .extracting(JsonNode::asText)
                .containsExactly(
                        expectedSeatIds.get(0).toString(), expectedSeatIds.get(1).toString());
    }

    @Test
    void serializationFailureShouldUseStableInventoryError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultSeatReleasedOutboxFactory failingFactory =
                new DefaultSeatReleasedOutboxFactory(failingMapper);

        ShowSeat showSeat =
                createShowSeat(
                        createShowtime(), "H7", SeatType.STANDARD, new BigDecimal("90000.00"));

        InternalServerException exception =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                failingFactory.create(
                                        UuidGenerator.next(),
                                        UuidGenerator.next(),
                                        List.of(showSeat),
                                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                                        RELEASED_AT,
                                        UuidGenerator.next(),
                                        UuidGenerator.next()));

        assertThat(exception.getErrorCode().code())
                .isEqualTo("INVENTORY_OUTBOX_PAYLOAD_SERIALIZATION_FAILED");
    }

    private Showtime createShowtime() {

        Cinema cinema = new Cinema("Seat Release Factory", "123 Main Street", "Ho Chi Minh City");

        Room room = new Room(cinema, "Release Room", RoomType.STANDARD);

        return new Showtime(
                UuidGenerator.next(),
                room,
                RELEASED_AT.plusDays(1),
                RELEASED_AT.plusDays(1).plusHours(2));
    }

    private ShowSeat createShowSeat(
            Showtime showtime, String seatNumber, SeatType seatType, BigDecimal price) {

        Seat seat = new Seat(showtime.getRoom(), seatNumber, seatNumber.substring(0, 1), seatType);

        return new ShowSeat(showtime, seat, price);
    }
}
