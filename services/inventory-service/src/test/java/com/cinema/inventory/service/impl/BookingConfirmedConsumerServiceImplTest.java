package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.cinema.inventory.event.serialization.BookingConfirmedPayloadReader;
import com.cinema.inventory.event.validation.BookingConfirmedMessageValidator;
import com.cinema.inventory.event.validation.BookingConfirmedPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingConfirmedConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BookingConfirmedConsumerServiceImplTest {

    private static final OffsetDateTime CONFIRMED_AT =
            OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final BigDecimal FIRST_SEAT_PRICE =
            new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE =
            new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT =
            new BigDecimal("210000.00");

    @Mock private BookingConfirmedMessageValidator messageValidator;

    @Mock private BookingConfirmedPayloadReader payloadReader;

    @Mock private BookingConfirmedPayloadValidator payloadValidator;

    @Mock
    private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private ShowSeatRepository showSeatRepository;

    private BookingConfirmedConsumerServiceImpl consumerService;

    @BeforeEach
    void setUp() {

        consumerService =
                new BookingConfirmedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        showSeatRepository);
    }

    @Test
    void canonicalConfirmationShouldBookCompleteHeldSeatSet() {

        TestContext context =
                contextWithHeldSeats();

        prepareValidatedMessage(context);

        when(
                        showSeatRepository
                                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                                        context.payload().showtimeId(),
                                        List.of("H7", "H8")))
                .thenReturn(context.showSeats());

        BookingConfirmedConsumerService.Result result =
                consumerService.handle(
                        context.bookingId().toString(),
                        context.message());

        assertThat(result.status())
                .isEqualTo(
                        BookingConfirmedConsumerService.Status.BOOKED);

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus())
                                    .isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId())
                                    .isNull();

                            assertThat(showSeat.getHoldExpiresAt())
                                    .isNull();
                        });

        verify(messageValidator)
                .validate(
                        context.bookingId().toString(),
                        context.message());

        verify(payloadReader).read(context.message());

        verify(payloadValidator)
                .validate(
                        context.bookingId().toString(),
                        context.message(),
                        context.payload());

        verify(processedEventRegistrationService)
                .register(
                        context.message().eventId(),
                        InventoryEventContract.BOOKING_CONFIRMED_CONSUMER,
                        InventoryEventContract.BOOKING_CONFIRMED,
                        InventoryEventContract.BOOKING_CONFIRMED_VERSION);

        verify(showSeatRepository).saveAll(context.showSeats());
    }

    @Test
    void duplicateEventShouldNotLockOrBookSeatsAgain() {

        TestContext context =
                contextWithHeldSeats();

        when(payloadReader.read(context.message()))
                .thenReturn(context.payload());

        when(
                        processedEventRegistrationService.register(
                                context.message().eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER,
                                InventoryEventContract.BOOKING_CONFIRMED,
                                InventoryEventContract.BOOKING_CONFIRMED_VERSION))
                .thenReturn(false);

        BookingConfirmedConsumerService.Result result =
                consumerService.handle(
                        context.bookingId().toString(),
                        context.message());

        assertThat(result.status())
                .isEqualTo(
                        BookingConfirmedConsumerService.Status.DUPLICATE);

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.HELD));

        verifyNoInteractions(showSeatRepository);
    }

    @Test
    void missingRequestedSeatShouldRejectWithoutPartialBooking() {

        TestContext context =
                contextWithHeldSeats();

        prepareValidatedMessage(context);

        when(
                        showSeatRepository
                                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                                        context.payload().showtimeId(),
                                        List.of("H7", "H8")))
                .thenReturn(List.of(context.showSeats().get(0)));

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(),
                                        context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.HELD));

        verify(showSeatRepository, never()).saveAll(
                context.showSeats());
    }

    @Test
    void seatHeldByAnotherBookingShouldRejectWithoutPartialBooking() {

        TestContext context =
                contextWithHeldSeats();

        ShowSeat firstSeat = context.showSeats().get(0);

        ShowSeat secondSeat =
                createHeldShowSeat(
                        context.showtime(),
                        "H8",
                        SeatType.VIP,
                        SECOND_SEAT_PRICE,
                        UuidGenerator.next());

        List<ShowSeat> repositorySeats =
                List.of(firstSeat, secondSeat);

        prepareValidatedMessage(context);

        when(
                        showSeatRepository
                                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                                        context.payload().showtimeId(),
                                        List.of("H7", "H8")))
                .thenReturn(repositorySeats);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(),
                                        context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(firstSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(firstSeat.getHeldByBookingId())
                .isEqualTo(context.bookingId());

        assertThat(secondSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(secondSeat.getHeldByBookingId())
                .isNotEqualTo(context.bookingId());

        verify(showSeatRepository, never()).saveAll(repositorySeats);
    }

    @Test
    void mismatchedSeatSnapshotShouldRejectWithoutPartialBooking() {

        TestContext context =
                contextWithHeldSeats();

        BookingConfirmedPayload mismatchedPayload =
                new BookingConfirmedPayload(
                        context.payload().bookingId(),
                        context.payload().userId(),
                        context.payload().showtimeId(),
                        context.payload().paymentId(),
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("80000.00")),
                                new ConfirmedSeatPayload(
                                        "H8",
                                        "VIP",
                                        SECOND_SEAT_PRICE)),
                        new BigDecimal("200000.00"),
                        context.payload().currency(),
                        context.payload().confirmedAt());

        when(payloadReader.read(context.message()))
                .thenReturn(mismatchedPayload);

        when(
                        processedEventRegistrationService.register(
                                context.message().eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER,
                                InventoryEventContract.BOOKING_CONFIRMED,
                                InventoryEventContract.BOOKING_CONFIRMED_VERSION))
                .thenReturn(true);

        when(
                        showSeatRepository
                                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                                        mismatchedPayload.showtimeId(),
                                        List.of("H7", "H8")))
                .thenReturn(context.showSeats());

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(),
                                        context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.showSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.HELD));

        verify(showSeatRepository, never()).saveAll(
                context.showSeats());
    }

    @Test
    void alreadyBookedSeatShouldRejectDistinctConfirmation() {

        TestContext context =
                contextWithHeldSeats();

        context.showSeats()
                .get(0)
                .book(context.bookingId());

        prepareValidatedMessage(context);

        when(
                        showSeatRepository
                                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                                        context.payload().showtimeId(),
                                        List.of("H7", "H8")))
                .thenReturn(context.showSeats());

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(),
                                        context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.showSeats().get(0).getStatus())
                .isEqualTo(ShowSeatStatus.BOOKED);

        assertThat(context.showSeats().get(1).getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        verify(showSeatRepository, never()).saveAll(
                context.showSeats());
    }

    private void prepareValidatedMessage(
            TestContext context) {

        when(payloadReader.read(context.message()))
                .thenReturn(context.payload());

        when(
                        processedEventRegistrationService.register(
                                context.message().eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER,
                                InventoryEventContract.BOOKING_CONFIRMED,
                                InventoryEventContract.BOOKING_CONFIRMED_VERSION))
                .thenReturn(true);
    }

    private TestContext contextWithHeldSeats() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        Showtime showtime = createShowtime();

        List<ShowSeat> showSeats =
                List.of(
                        createHeldShowSeat(
                                showtime,
                                "H7",
                                SeatType.STANDARD,
                                FIRST_SEAT_PRICE,
                                bookingId),
                        createHeldShowSeat(
                                showtime,
                                "H8",
                                SeatType.VIP,
                                SECOND_SEAT_PRICE,
                                bookingId));

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        paymentId,
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7",
                                        "STANDARD",
                                        FIRST_SEAT_PRICE),
                                new ConfirmedSeatPayload(
                                        "H8",
                                        "VIP",
                                        SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        InventoryEventContract.CURRENCY_VND,
                        CONFIRMED_AT);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                        InventoryEventContract.BOOKING_CONFIRMED,
                        InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                        CONFIRMED_AT,
                        InventoryEventContract.BOOKING_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(
                bookingId,
                showtime,
                showSeats,
                payload,
                message);
    }

    private Showtime createShowtime() {

        Cinema cinema =
                new Cinema(
                        "Cinema One",
                        "123 Main Street",
                        "Ho Chi Minh City");

        Room room =
                new Room(
                        cinema,
                        "Room 1",
                        RoomType.STANDARD);

        return new Showtime(
                UuidGenerator.next(),
                room,
                CONFIRMED_AT.plusDays(1),
                CONFIRMED_AT.plusDays(1).plusHours(2));
    }

    private ShowSeat createHeldShowSeat(
            Showtime showtime,
            String seatNumber,
            SeatType seatType,
            BigDecimal price,
            UUID bookingId) {

        Seat seat =
                new Seat(
                        showtime.getRoom(),
                        seatNumber,
                        seatNumber.substring(0, 1),
                        seatType);

        ShowSeat showSeat =
                new ShowSeat(
                        showtime,
                        seat,
                        price);

        showSeat.hold(
                bookingId,
                CONFIRMED_AT.plusMinutes(10),
                CONFIRMED_AT.minusMinutes(1));

        return showSeat;
    }

    private record TestContext(
            UUID bookingId,
            Showtime showtime,
            List<ShowSeat> showSeats,
            BookingConfirmedPayload payload,
            OutboxEventMessage message) {}
}
