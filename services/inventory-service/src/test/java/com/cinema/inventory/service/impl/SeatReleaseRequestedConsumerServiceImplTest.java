package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.SeatReleasedOutboxFactory;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.event.serialization.SeatReleaseRequestedPayloadReader;
import com.cinema.inventory.event.validation.SeatReleaseRequestedMessageValidator;
import com.cinema.inventory.event.validation.SeatReleaseRequestedPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.ProcessedEventRegistrationService;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SeatReleaseRequestedConsumerServiceImplTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-15T10:01:00Z");

    private static final OffsetDateTime RELEASED_AT =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final Clock CLOCK = Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);

    @Mock private SeatReleaseRequestedMessageValidator messageValidator;

    @Mock private SeatReleaseRequestedPayloadReader payloadReader;

    @Mock private SeatReleaseRequestedPayloadValidator payloadValidator;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private ShowSeatRepository showSeatRepository;

    @Mock private SeatReleasedOutboxFactory seatReleasedOutboxFactory;

    @Mock private OutboxService outboxService;

    private SeatReleaseRequestedConsumerServiceImpl consumerService;

    @BeforeEach
    void setUp() {

        consumerService =
                new SeatReleaseRequestedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        showSeatRepository,
                        seatReleasedOutboxFactory,
                        outboxService,
                        CLOCK);
    }

    @Test
    void canonicalRequestShouldReleaseCompleteHeldSeatSet() {

        TestContext context = contextWithHeldSeats();

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        UUID outboxEventId = UuidGenerator.next();

        when(outboxEvent.getId()).thenReturn(outboxEventId);

        prepareRegisteredMessage(context);

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        context.payload().showtimeId(), context.sortedSeatIds()))
                .thenReturn(context.showSeats());

        when(seatReleasedOutboxFactory.create(
                        context.bookingId(),
                        context.payload().showtimeId(),
                        context.showSeats(),
                        context.payload().reason(),
                        RELEASED_AT,
                        context.message().correlationId(),
                        context.message().eventId()))
                .thenReturn(outboxEvent);

        SeatReleaseRequestedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), context.message());

        assertThat(result.status()).isEqualTo(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertThat(result.outboxEventId()).isEqualTo(outboxEventId);

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        verify(messageValidator).validate(context.bookingId().toString(), context.message());

        verify(payloadReader).read(context.message());

        verify(payloadValidator)
                .validate(context.bookingId().toString(), context.message(), context.payload());

        verify(processedEventRegistrationService)
                .register(
                        context.message().eventId(),
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION);

        verify(showSeatRepository).saveAll(context.showSeats());

        verify(outboxService).save(outboxEvent);
    }

    @Test
    void duplicateEventShouldNotLockOrReleaseSeatsAgain() {

        TestContext context = contextWithHeldSeats();

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION))
                .thenReturn(false);

        SeatReleaseRequestedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), context.message());

        assertThat(result.status()).isEqualTo(SeatReleaseRequestedConsumerService.Status.DUPLICATE);

        assertThat(result.outboxEventId()).isNull();

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD));

        verifyNoInteractions(showSeatRepository);

        verifyNoInteractions(seatReleasedOutboxFactory);

        verifyNoInteractions(outboxService);
    }

    @Test
    void missingRequestedSeatShouldRejectWithoutPartialRelease() {

        TestContext context = contextWithHeldSeats();

        prepareRegisteredMessage(context);

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        context.payload().showtimeId(), context.sortedSeatIds()))
                .thenReturn(List.of(context.showSeats().get(0)));

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertStillHeld(context.showSeats(), context.bookingId());

        verify(showSeatRepository, never()).saveAll(context.showSeats());

        verifyNoInteractions(seatReleasedOutboxFactory);

        verifyNoInteractions(outboxService);
    }

    @Test
    void seatHeldByAnotherBookingShouldRejectWithoutPartialRelease() {

        TestContext context = contextWithHeldSeats();

        ShowSeat firstSeat = context.showSeats().get(0);

        ShowSeat secondSeat =
                createHeldShowSeat(
                        context.showtime(),
                        "H8",
                        SeatType.VIP,
                        new BigDecimal("120000.00"),
                        UuidGenerator.next());

        List<ShowSeat> repositorySeats = List.of(firstSeat, secondSeat);

        List<UUID> requestedIds = repositorySeats.stream().map(ShowSeat::getId).sorted().toList();

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        context.bookingId(),
                        context.showtime().getId(),
                        requestedIds,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        context.payload().requestedAt());

        when(payloadReader.read(context.message())).thenReturn(payload);

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION))
                .thenReturn(true);

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        payload.showtimeId(), requestedIds))
                .thenReturn(repositorySeats);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(firstSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

        assertThat(firstSeat.getHeldByBookingId()).isEqualTo(context.bookingId());

        assertThat(secondSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

        assertThat(secondSeat.getHeldByBookingId()).isNotEqualTo(context.bookingId());

        verify(showSeatRepository, never()).saveAll(repositorySeats);

        verifyNoInteractions(seatReleasedOutboxFactory);

        verifyNoInteractions(outboxService);
    }

    @Test
    void bookedSeatShouldRejectWithoutReleasingRemainingSeat() {

        TestContext context = contextWithHeldSeats();

        context.showSeats().get(0).book(context.bookingId());

        prepareRegisteredMessage(context);

        when(showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        context.payload().showtimeId(), context.sortedSeatIds()))
                .thenReturn(context.showSeats());

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.showSeats().get(0).getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

        assertThat(context.showSeats().get(1).getStatus()).isEqualTo(ShowSeatStatus.HELD);

        assertThat(context.showSeats().get(1).getHeldByBookingId()).isEqualTo(context.bookingId());

        verify(showSeatRepository, never()).saveAll(context.showSeats());

        verifyNoInteractions(seatReleasedOutboxFactory);

        verifyNoInteractions(outboxService);
    }

    private void prepareRegisteredMessage(TestContext context) {

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION))
                .thenReturn(true);
    }

    private TestContext contextWithHeldSeats() {

        UUID bookingId = UuidGenerator.next();

        Showtime showtime = createShowtime();

        List<ShowSeat> showSeats =
                List.of(
                        createHeldShowSeat(
                                showtime,
                                "H7",
                                SeatType.STANDARD,
                                new BigDecimal("90000.00"),
                                bookingId),
                        createHeldShowSeat(
                                showtime,
                                "H8",
                                SeatType.VIP,
                                new BigDecimal("120000.00"),
                                bookingId));

        List<UUID> sortedSeatIds =
                showSeats.stream().map(ShowSeat::getId).sorted(Comparator.naturalOrder()).toList();

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        showtime.getId(),
                        List.of(sortedSeatIds.get(1), sortedSeatIds.get(0)),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        OffsetDateTime.parse("2026-09-15T10:00:00Z"));

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED,
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                        payload.requestedAt(),
                        InventoryEventContract.BOOKING_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(bookingId, showtime, showSeats, sortedSeatIds, payload, message);
    }

    private Showtime createShowtime() {

        Cinema cinema = new Cinema("Seat Release Consumer", "123 Main Street", "Ho Chi Minh City");

        Room room = new Room(cinema, "Release Room", RoomType.STANDARD);

        return new Showtime(
                UuidGenerator.next(),
                room,
                RELEASED_AT.plusDays(1),
                RELEASED_AT.plusDays(1).plusHours(2));
    }

    private ShowSeat createHeldShowSeat(
            Showtime showtime,
            String seatNumber,
            SeatType seatType,
            BigDecimal price,
            UUID bookingId) {

        Seat seat = new Seat(showtime.getRoom(), seatNumber, seatNumber.substring(0, 1), seatType);

        ShowSeat showSeat = new ShowSeat(showtime, seat, price);

        showSeat.hold(bookingId, RELEASED_AT.plusMinutes(10), RELEASED_AT.minusMinutes(1));

        return showSeat;
    }

    private void assertStillHeld(List<ShowSeat> showSeats, UUID bookingId) {

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId()).isEqualTo(bookingId);
                        });
    }

    private record TestContext(
            UUID bookingId,
            Showtime showtime,
            List<ShowSeat> showSeats,
            List<UUID> sortedSeatIds,
            SeatReleaseRequestedPayload payload,
            OutboxEventMessage message) {}
}
