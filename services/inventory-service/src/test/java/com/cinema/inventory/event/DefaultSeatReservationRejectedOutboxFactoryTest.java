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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class DefaultSeatReservationRejectedOutboxFactoryTest {

    private static final String REJECTION_MESSAGE = "One or more requested seats are unavailable";

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultSeatReservationRejectedOutboxFactory factory =
            new DefaultSeatReservationRejectedOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalSeatReservationRejectedOutboxEvent() throws Exception {

        OffsetDateTime rejectedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        OutboxEventEntity event =
                factory.create(
                        bookingId,
                        showtimeId,
                        SeatReservationRejectionReason.SEAT_UNAVAILABLE,
                        REJECTION_MESSAGE,
                        List.of("H8", "H7"),
                        rejectedAt,
                        correlationId,
                        causationId);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.BOOKING);

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getEventType())
                .isEqualTo(InventoryEventContract.SEAT_RESERVATION_REJECTED);

        assertThat(event.getEventVersion())
                .isEqualTo(InventoryEventContract.SEAT_RESERVATION_REJECTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(InventoryEventContract.SEAT_RESERVATION_REJECTED);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getOccurredAt()).isEqualTo(rejectedAt);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(causationId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(rejectedAt);

        assertThat(event.getCreatedAt()).isEqualTo(rejectedAt);

        assertThat(event.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.get("reasonCode").asText())
                .isEqualTo(SeatReservationRejectionReason.SEAT_UNAVAILABLE.name());

        assertThat(payload.get("message").asText()).isEqualTo(REJECTION_MESSAGE);

        assertThat(OffsetDateTime.parse(payload.get("rejectedAt").asText())).isEqualTo(rejectedAt);

        assertThat(payload.get("unavailableSeats"))
                .extracting(JsonNode::asText)
                .containsExactly("H7", "H8");
    }

    @Test
    void nullUnavailableSeatsShouldBecomeEmptyArray() throws Exception {

        OffsetDateTime rejectedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");

        OutboxEventEntity event =
                factory.create(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        SeatReservationRejectionReason.INVALID_REQUEST,
                        "Reservation request is invalid",
                        null,
                        rejectedAt,
                        UuidGenerator.next(),
                        UuidGenerator.next());

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("unavailableSeats")).isNotNull();

        assertThat(payload.get("unavailableSeats").isArray()).isTrue();

        assertThat(payload.get("unavailableSeats")).isEmpty();
    }

    @Test
    void serializationFailureShouldUseStableInventoryError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultSeatReservationRejectedOutboxFactory failingFactory =
                new DefaultSeatReservationRejectedOutboxFactory(failingMapper);

        InternalServerException exception =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                failingFactory.create(
                                        UuidGenerator.next(),
                                        UuidGenerator.next(),
                                        SeatReservationRejectionReason.INVENTORY_CONFLICT,
                                        "Inventory conflict",
                                        List.of("H7"),
                                        OffsetDateTime.parse("2026-08-21T10:00:00Z"),
                                        UuidGenerator.next(),
                                        UuidGenerator.next()));

        assertThat(exception.getErrorCode().code())
                .isEqualTo("INVENTORY_OUTBOX_PAYLOAD_SERIALIZATION_FAILED");
    }
}
