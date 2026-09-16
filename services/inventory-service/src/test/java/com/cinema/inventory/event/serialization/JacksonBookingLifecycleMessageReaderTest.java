package com.cinema.inventory.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonBookingLifecycleMessageReaderTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-16T08:30:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonBookingCancelledMessageReader cancelledReader =
            new JacksonBookingCancelledMessageReader(objectMapper);

    private final JacksonBookingExpiredMessageReader expiredReader =
            new JacksonBookingExpiredMessageReader(objectMapper);

    @Test
    void shouldReadCanonicalBookingCancelledMessage() throws Exception {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                message(
                        bookingId,
                        InventoryEventContract.BOOKING_CANCELLED,
                        InventoryEventContract.BOOKING_CANCELLED_VERSION);

        OutboxEventMessage result = cancelledReader.read(objectMapper.writeValueAsString(source));

        assertCanonicalEnvelope(
                result,
                source,
                InventoryEventContract.BOOKING_CANCELLED,
                InventoryEventContract.BOOKING_CANCELLED_VERSION);
    }

    @Test
    void shouldReadCanonicalBookingExpiredMessage() throws Exception {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage source =
                message(
                        bookingId,
                        InventoryEventContract.BOOKING_EXPIRED,
                        InventoryEventContract.BOOKING_EXPIRED_VERSION);

        OutboxEventMessage result = expiredReader.read(objectMapper.writeValueAsString(source));

        assertCanonicalEnvelope(
                result,
                source,
                InventoryEventContract.BOOKING_EXPIRED,
                InventoryEventContract.BOOKING_EXPIRED_VERSION);
    }

    @Test
    void cancelledReaderShouldRejectNullBlankAndMalformedMessages() {

        assertThatThrownBy(() -> cancelledReader.read(null))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> cancelledReader.read(" ")).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> cancelledReader.read("{\"payload\":"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void expiredReaderShouldRejectNullBlankAndMalformedMessages() {

        assertThatThrownBy(() -> expiredReader.read(null)).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> expiredReader.read(" ")).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> expiredReader.read("{\"payload\":"))
                .isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage message(UUID bookingId, String eventType, String eventVersion) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                eventType,
                eventVersion,
                OCCURRED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                null,
                JsonNodeFactory.instance.objectNode().put("bookingId", bookingId.toString()));
    }

    private void assertCanonicalEnvelope(
            OutboxEventMessage result,
            OutboxEventMessage source,
            String eventType,
            String eventVersion) {

        assertThat(result.eventId()).isEqualTo(source.eventId());

        assertThat(result.aggregateId()).isEqualTo(source.aggregateId());

        assertThat(result.aggregateType()).isEqualTo(InventoryEventContract.BOOKING_AGGREGATE_TYPE);

        assertThat(result.eventType()).isEqualTo(eventType);

        assertThat(result.eventVersion()).isEqualTo(eventVersion);

        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);

        assertThat(result.producer()).isEqualTo(InventoryEventContract.BOOKING_PRODUCER);

        assertThat(result.correlationId()).isEqualTo(source.correlationId());

        /*
         * Booking cancellation/expiration originate from Booking-owned
         * lifecycle actions and currently have no source event.
         */
        assertThat(result.causationId()).isNull();

        assertThat(result.payload().path("bookingId").asText())
                .isEqualTo(source.aggregateId().toString());
    }
}
