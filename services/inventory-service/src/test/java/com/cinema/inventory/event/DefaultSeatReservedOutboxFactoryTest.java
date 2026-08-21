package com.cinema.inventory.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import java.util.UUID;

class DefaultSeatReservedOutboxFactoryTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultSeatReservedOutboxFactory factory =
            new DefaultSeatReservedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalSeatReservedOutboxEvent() throws Exception {

        OffsetDateTime heldAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        OffsetDateTime holdExpiresAt = heldAt.plusMinutes(10);

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        Showtime showtime = createShowtime(showtimeId, heldAt);

        ShowSeat h8 = createShowSeat(showtime, "H8", SeatType.VIP, new BigDecimal("120000.00"));

        ShowSeat h7 = createShowSeat(showtime, "H7", SeatType.STANDARD, new BigDecimal("90000.00"));

        h8.hold(bookingId, holdExpiresAt, heldAt);
        h7.hold(bookingId, holdExpiresAt, heldAt);

        OutboxEventEntity event =
                factory.create(
                        bookingId,
                        showtimeId,
                        List.of(h8, h7),
                        heldAt,
                        holdExpiresAt,
                        correlationId,
                        causationId);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getEventType()).isEqualTo(InventoryEventContract.SEAT_RESERVED);

        assertThat(event.getEventVersion()).isEqualTo(InventoryEventContract.SEAT_RESERVED_VERSION);

        assertThat(event.getTopic()).isEqualTo(InventoryEventContract.SEAT_RESERVED);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getOccurredAt()).isEqualTo(heldAt);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(causationId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(heldAt);

        assertThat(event.getCreatedAt()).isEqualTo(heldAt);

        assertThat(event.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.get("currency").asText()).isEqualTo(InventoryEventContract.CURRENCY_VND);

        assertThat(payload.get("totalAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("210000.00"));

        assertThat(OffsetDateTime.parse(payload.get("heldAt").asText())).isEqualTo(heldAt);

        assertThat(OffsetDateTime.parse(payload.get("holdExpiresAt").asText()))
                .isEqualTo(holdExpiresAt);

        JsonNode seats = payload.get("seats");

        assertThat(seats).hasSize(2);

        assertThat(seats)
                .extracting(seat -> seat.get("seatNumber").asText())
                .containsExactly("H7", "H8");

        JsonNode h7Payload = seats.get(0);

        assertThat(h7Payload.get("inventorySeatId").asText()).isEqualTo(h7.getId().toString());

        assertThat(h7Payload.get("seatNumber").asText()).isEqualTo("H7");

        assertThat(h7Payload.get("seatType").asText()).isEqualTo(SeatType.STANDARD.name());

        assertThat(h7Payload.get("price").decimalValue())
                .isEqualByComparingTo(new BigDecimal("90000.00"));

        JsonNode h8Payload = seats.get(1);

        assertThat(h8Payload.get("inventorySeatId").asText()).isEqualTo(h8.getId().toString());

        assertThat(h8Payload.get("seatNumber").asText()).isEqualTo("H8");

        assertThat(h8Payload.get("seatType").asText()).isEqualTo(SeatType.VIP.name());

        assertThat(h8Payload.get("price").decimalValue())
                .isEqualByComparingTo(new BigDecimal("120000.00"));
    }

    @Test
    void serializationFailureShouldUseStableInventoryError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultSeatReservedOutboxFactory failingFactory =
                new DefaultSeatReservedOutboxFactory(failingMapper);

        OffsetDateTime heldAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        OffsetDateTime holdExpiresAt = heldAt.plusMinutes(10);

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Showtime showtime = createShowtime(showtimeId, heldAt);

        ShowSeat showSeat =
                createShowSeat(showtime, "H7", SeatType.STANDARD, new BigDecimal("90000.00"));

        showSeat.hold(bookingId, holdExpiresAt, heldAt);

        InternalServerException exception =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                failingFactory.create(
                                        bookingId,
                                        showtimeId,
                                        List.of(showSeat),
                                        heldAt,
                                        holdExpiresAt,
                                        UuidGenerator.next(),
                                        UuidGenerator.next()));

        assertThat(exception.getErrorCode().code())
                .isEqualTo("INVENTORY_OUTBOX_PAYLOAD_SERIALIZATION_FAILED");
    }

    private Showtime createShowtime(UUID ignoredShowtimeId, OffsetDateTime now) {

        Cinema cinema = new Cinema("Cinema Factory Test", "123 Main Street", "Ho Chi Minh City");

        Room room = new Room(cinema, "Room 1", RoomType.STANDARD);

        /*
         * BaseEntity tự tạo UUID v7. Tham số showtimeId của factory
         * là external reference cần kiểm tra trong payload, không cần
         * ép ID nội bộ của fixture bằng reflection.
         */
        return new Showtime(
                UuidGenerator.next(), room, now.plusDays(1), now.plusDays(1).plusHours(2));
    }

    private ShowSeat createShowSeat(
            Showtime showtime, String seatNumber, SeatType seatType, BigDecimal price) {

        Room room = showtime.getRoom();

        Seat seat = new Seat(room, seatNumber, seatNumber.substring(0, 1), seatType);

        return new ShowSeat(showtime, seat, price);
    }
}
