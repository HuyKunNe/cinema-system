package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.cinema.inventory.event.serialization.BookingExpiredPayloadReader;
import com.cinema.inventory.event.validation.BookingExpiredMessageValidator;
import com.cinema.inventory.event.validation.BookingExpiredPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingExpiredConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BookingExpiredConsumerServiceImplTest {

    private static final String PARTITION_KEY = "booking-partition";

    @Mock private BookingExpiredMessageValidator messageValidator;

    @Mock private BookingExpiredPayloadReader payloadReader;

    @Mock private BookingExpiredPayloadValidator payloadValidator;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private ShowSeatRepository showSeatRepository;

    @Mock private ShowSeat firstShowSeat;

    @Mock private ShowSeat secondShowSeat;

    private BookingExpiredConsumerServiceImpl consumerService;

    @BeforeEach
    void setUp() {
        consumerService =
                new BookingExpiredConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        showSeatRepository);
    }

    @Test
    void canonicalMessageShouldReleaseHeldSeats() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        bookingId, UuidGenerator.next(), showtimeId, message.occurredAt());

        when(payloadReader.read(message)).thenReturn(payload);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_EXPIRED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        when(showSeatRepository.findAllHeldByBookingForUpdate(showtimeId, bookingId))
                .thenReturn(List.of(firstShowSeat, secondShowSeat));

        BookingExpiredConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        verify(messageValidator).validate(PARTITION_KEY, message);

        verify(payloadReader).read(message);

        verify(payloadValidator).validate(PARTITION_KEY, message, payload);

        verify(processedEventRegistrationService)
                .register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_EXPIRED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        verify(showSeatRepository).findAllHeldByBookingForUpdate(showtimeId, bookingId);

        verify(firstShowSeat).release(bookingId);

        verify(secondShowSeat).release(bookingId);

        verify(showSeatRepository).saveAll(List.of(firstShowSeat, secondShowSeat));
    }

    @Test
    void duplicateMessageShouldReturnDuplicateWithoutLockingSeats() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        bookingId,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        message.occurredAt());

        when(payloadReader.read(message)).thenReturn(payload);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_EXPIRED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(false);

        BookingExpiredConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingExpiredConsumerService.Status.DUPLICATE);

        verify(messageValidator).validate(PARTITION_KEY, message);

        verify(payloadReader).read(message);

        verify(payloadValidator).validate(PARTITION_KEY, message, payload);

        verifyNoInteractions(showSeatRepository);

        verify(firstShowSeat, never()).release(bookingId);

        verify(secondShowSeat, never()).release(bookingId);
    }

    @Test
    void noHeldSeatsShouldStillCompleteRelease() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        bookingId, UuidGenerator.next(), showtimeId, message.occurredAt());

        when(payloadReader.read(message)).thenReturn(payload);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_EXPIRED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        when(showSeatRepository.findAllHeldByBookingForUpdate(showtimeId, bookingId))
                .thenReturn(List.of());

        BookingExpiredConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        verify(showSeatRepository).findAllHeldByBookingForUpdate(showtimeId, bookingId);

        verify(showSeatRepository).saveAll(List.of());

        verifyNoInteractions(firstShowSeat, secondShowSeat);
    }

    private OutboxEventMessage message(UUID bookingId) {

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-16T10:00:00Z");

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_EXPIRED,
                InventoryEventContract.BOOKING_EXPIRED_VERSION,
                occurredAt,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                null);
    }
}
