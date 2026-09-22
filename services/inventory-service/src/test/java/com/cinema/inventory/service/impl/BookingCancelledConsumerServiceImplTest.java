package com.cinema.inventory.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.serialization.BookingCancelledPayloadReader;
import com.cinema.inventory.event.validation.BookingCancelledMessageValidator;
import com.cinema.inventory.event.validation.BookingCancelledPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingCancelledConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

@ExtendWith(MockitoExtension.class)
class BookingCancelledConsumerServiceImplTest {

    private static final String PARTITION_KEY = "booking-partition";

    @Mock private BookingCancelledMessageValidator messageValidator;

    @Mock private BookingCancelledPayloadReader payloadReader;

    @Mock private BookingCancelledPayloadValidator payloadValidator;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private ShowSeatRepository showSeatRepository;

    @Mock private ShowSeat firstShowSeat;

    @Mock private ShowSeat secondShowSeat;

    private BookingCancelledConsumerServiceImpl consumerService;

    @BeforeEach
    void setUp() {

        consumerService =
                new BookingCancelledConsumerServiceImpl(
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

        UUID firstShowSeatId = UuidGenerator.next();
        UUID secondShowSeatId = UuidGenerator.next();

        List<UUID> sortedShowSeatIds =
                List.of(firstShowSeatId, secondShowSeatId).stream().sorted().toList();

        OutboxEventMessage message = message(bookingId);

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        message.occurredAt());

        doReturn(payload).when(payloadReader).read(message);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        when(showSeatRepository.findIdsByShowtimeIdAndBookingId(showtimeId, bookingId))
                .thenReturn(List.of(firstShowSeatId, secondShowSeatId));

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(showtimeId, sortedShowSeatIds))
                .thenReturn(List.of(firstShowSeat, secondShowSeat));

        when(firstShowSeat.isHeldBy(bookingId)).thenReturn(true);

        when(secondShowSeat.isHeldBy(bookingId)).thenReturn(true);

        BookingCancelledConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        verify(messageValidator).validate(PARTITION_KEY, message);

        verify(payloadReader).read(message);

        verify(payloadValidator).validate(PARTITION_KEY, message, payload);

        verify(processedEventRegistrationService)
                .register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        verify(showSeatRepository).findIdsByShowtimeIdAndBookingId(showtimeId, bookingId);

        verify(showSeatRepository)
                .findAllByShowtimeIdAndIdsForUpdate(showtimeId, sortedShowSeatIds);

        verify(firstShowSeat).isHeldBy(bookingId);

        verify(secondShowSeat).isHeldBy(bookingId);

        verify(firstShowSeat).release(bookingId);

        verify(secondShowSeat).release(bookingId);

        verify(showSeatRepository).saveAll(List.of(firstShowSeat, secondShowSeat));
    }

    @Test
    void duplicateMessageShouldReturnDuplicateWithoutLockingSeats() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        message.occurredAt());

        doReturn(payload).when(payloadReader).read(message);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(false);

        BookingCancelledConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.DUPLICATE);

        verify(messageValidator).validate(PARTITION_KEY, message);

        verify(payloadReader).read(message);

        verify(payloadValidator).validate(PARTITION_KEY, message, payload);

        verify(processedEventRegistrationService)
                .register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        verifyNoInteractions(showSeatRepository);

        verify(firstShowSeat, never()).release(bookingId);

        verify(secondShowSeat, never()).release(bookingId);
    }

    @Test
    void noHeldSeatsShouldStillCompleteRelease() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        message.occurredAt());

        doReturn(payload).when(payloadReader).read(message);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        when(showSeatRepository.findIdsByShowtimeIdAndBookingId(showtimeId, bookingId))
                .thenReturn(List.of());

        BookingCancelledConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        verify(showSeatRepository).findIdsByShowtimeIdAndBookingId(showtimeId, bookingId);

        verify(showSeatRepository, never())
                .findAllByShowtimeIdAndIdsForUpdate(showtimeId, List.of());

        verify(showSeatRepository, never()).saveAll(List.of());

        verifyNoInteractions(firstShowSeat, secondShowSeat);
    }

    @Test
    void seatAlreadyReleasedAfterIdLookupShouldBeIgnored() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();
        UUID showSeatId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        message.occurredAt());

        doReturn(payload).when(payloadReader).read(message);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        /*
         * Seat vẫn thuộc booking tại thời điểm query ID.
         */
        when(showSeatRepository.findIdsByShowtimeIdAndBookingId(showtimeId, bookingId))
                .thenReturn(List.of(showSeatId));

        /*
         * Sau khi acquire pessimistic lock,
         * transaction khác đã release seat trước đó.
         */
        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(showtimeId, List.of(showSeatId)))
                .thenReturn(List.of(firstShowSeat));

        when(firstShowSeat.isHeldBy(bookingId)).thenReturn(false);

        BookingCancelledConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        verify(showSeatRepository).findIdsByShowtimeIdAndBookingId(showtimeId, bookingId);

        verify(showSeatRepository)
                .findAllByShowtimeIdAndIdsForUpdate(showtimeId, List.of(showSeatId));

        verify(firstShowSeat).isHeldBy(bookingId);

        verify(firstShowSeat, never()).release(bookingId);

        /*
         * Nếu implementation hiện tại vẫn gọi
         * saveAll(releasableShowSeats),
         * thì list ở đây sẽ empty.
         */
        verify(showSeatRepository).saveAll(List.of());

        verifyNoInteractions(secondShowSeat);
    }

    @Test
    void onlySeatsStillHeldByBookingShouldBeReleased() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        UUID firstShowSeatId = UuidGenerator.next();
        UUID secondShowSeatId = UuidGenerator.next();

        List<UUID> sortedShowSeatIds =
                List.of(firstShowSeatId, secondShowSeatId).stream().sorted().toList();

        OutboxEventMessage message = message(bookingId);

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        message.occurredAt());

        doReturn(payload).when(payloadReader).read(message);

        when(processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion()))
                .thenReturn(true);

        when(showSeatRepository.findIdsByShowtimeIdAndBookingId(showtimeId, bookingId))
                .thenReturn(List.of(firstShowSeatId, secondShowSeatId));

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(showtimeId, sortedShowSeatIds))
                .thenReturn(List.of(firstShowSeat, secondShowSeat));

        when(firstShowSeat.isHeldBy(bookingId)).thenReturn(true);

        when(secondShowSeat.isHeldBy(bookingId)).thenReturn(false);

        BookingCancelledConsumerService.Result result =
                consumerService.handle(PARTITION_KEY, message);

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        verify(firstShowSeat).release(bookingId);

        verify(secondShowSeat, never()).release(bookingId);

        verify(showSeatRepository).saveAll(List.of(firstShowSeat));
    }

    private OutboxEventMessage message(UUID bookingId) {

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-16T10:00:00Z");

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CANCELLED,
                InventoryEventContract.BOOKING_CANCELLED_VERSION,
                occurredAt,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                null);
    }
}
