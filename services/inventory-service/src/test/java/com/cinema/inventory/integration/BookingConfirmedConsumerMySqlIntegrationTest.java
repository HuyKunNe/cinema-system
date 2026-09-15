package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
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
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.BookingConfirmedConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class BookingConfirmedConsumerMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final OffsetDateTime CONFIRMED_AT =
            OffsetDateTime.of(2026, 9, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired private BookingConfirmedConsumerService consumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    private ExecutorService executorService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        executorService = Executors.newFixedThreadPool(2);

        context = createHeldInventory();
    }

    @AfterEach
    void tearDown() throws InterruptedException {

        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {

            executorService.shutdownNow();
        }

        cleanDatabase();
    }

    @Test
    void canonicalConfirmationShouldBookCompleteSeatSet() {

        OutboxEventMessage message = bookingConfirmedMessage(UuidGenerator.next());

        BookingConfirmedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        List<ShowSeat> showSeats = loadShowSeats();

        assertThat(result.status()).isEqualTo(BookingConfirmedConsumerService.Status.BOOKED);

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isTrue();

        /*
         * booking-confirmed is a terminal Inventory command.
         * Inventory does not emit another event for this transition.
         */
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateConfirmationShouldBookOnlyOnce() {

        OutboxEventMessage message = bookingConfirmedMessage(UuidGenerator.next());

        BookingConfirmedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), message);

        BookingConfirmedConsumerService.Result duplicate =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(BookingConfirmedConsumerService.Status.BOOKED);

        assertThat(duplicate.status()).isEqualTo(BookingConfirmedConsumerService.Status.DUPLICATE);

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void distinctDelayedConfirmationShouldNotBookAgain() {

        OutboxEventMessage firstMessage = bookingConfirmedMessage(UuidGenerator.next());

        OutboxEventMessage delayedMessage = bookingConfirmedMessage(UuidGenerator.next());

        BookingConfirmedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), firstMessage);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), delayedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(BookingConfirmedConsumerService.Status.BOOKED);

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED));

        /*
         * The delayed event registered its marker before locking the seats,
         * but that marker must roll back with its rejected transition.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                firstMessage.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedMessage.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void concurrentDuplicateConfirmationShouldBookOnce() throws Exception {

        OutboxEventMessage message = bookingConfirmedMessage(UuidGenerator.next());

        List<ConcurrentOutcome> outcomes = executeConcurrently(message, message);

        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failure()).isNull());

        assertThat(outcomes)
                .extracting(ConcurrentOutcome::status)
                .containsExactlyInAnyOrder(
                        BookingConfirmedConsumerService.Status.BOOKED,
                        BookingConfirmedConsumerService.Status.DUPLICATE);

        entityManager.clear();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED));

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void concurrentDistinctConfirmationsShouldHaveOneWinner() throws Exception {

        OutboxEventMessage firstMessage = bookingConfirmedMessage(UuidGenerator.next());

        OutboxEventMessage secondMessage = bookingConfirmedMessage(UuidGenerator.next());

        List<ConcurrentOutcome> outcomes = executeConcurrently(firstMessage, secondMessage);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() == null)
                .extracting(ConcurrentOutcome::status)
                .containsExactly(BookingConfirmedConsumerService.Status.BOOKED);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() != null)
                .singleElement()
                .satisfies(
                        outcome ->
                                assertThat(outcome.failure())
                                        .isInstanceOf(ConflictException.class));

        entityManager.clear();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        List.of(
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        firstMessage.eventId(),
                                        InventoryEventContract.BOOKING_CONFIRMED_CONSUMER),
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        secondMessage.eventId(),
                                        InventoryEventContract.BOOKING_CONFIRMED_CONSUMER)))
                .containsExactlyInAnyOrder(true, false);

        assertThat(outboxRepository.count()).isZero();
    }

    private List<ConcurrentOutcome> executeConcurrently(
            OutboxEventMessage firstMessage, OutboxEventMessage secondMessage) throws Exception {

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<ConcurrentOutcome> first =
                executorService.submit(() -> handleConcurrently(firstMessage, ready, start));

        Future<ConcurrentOutcome> second =
                executorService.submit(() -> handleConcurrently(secondMessage, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }

    private ConcurrentOutcome handleConcurrently(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return ConcurrentOutcome.failure(
                        new IllegalStateException(
                                "Timed out waiting to start booking confirmation"));
            }

            BookingConfirmedConsumerService.Result result =
                    consumerService.handle(context.bookingId().toString(), message);

            return ConcurrentOutcome.success(result.status());

        } catch (Throwable failure) {

            return ConcurrentOutcome.failure(failure);
        }
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Confirmation Test",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Confirmation Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        CONFIRMED_AT.plusDays(1),
                        CONFIRMED_AT.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat firstShowSeat = new ShowSeat(savedShowtime, h7, STANDARD_PRICE);

        firstShowSeat.hold(bookingId, CONFIRMED_AT.plusMinutes(10), CONFIRMED_AT.minusMinutes(1));

        ShowSeat secondShowSeat = new ShowSeat(savedShowtime, h8, VIP_PRICE);

        secondShowSeat.hold(bookingId, CONFIRMED_AT.plusMinutes(10), CONFIRMED_AT.minusMinutes(1));

        showSeatRepository.saveAllAndFlush(List.of(firstShowSeat, secondShowSeat));

        entityManager.clear();

        return new TestContext(bookingId, userId, savedShowtime.getId(), paymentId);
    }

    private OutboxEventMessage bookingConfirmedMessage(UUID eventId) {

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        context.bookingId(),
                        context.userId(),
                        context.showtimeId(),
                        context.paymentId(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "STANDARD", STANDARD_PRICE),
                                new ConfirmedSeatPayload("H8", "VIP", VIP_PRICE)),
                        TOTAL_AMOUNT,
                        InventoryEventContract.CURRENCY_VND,
                        CONFIRMED_AT);

        return new OutboxEventMessage(
                eventId,
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                CONFIRMED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private List<ShowSeat> loadShowSeats() {

        return showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        showSeatRepository.deleteAllInBatch();

        showtimeRepository.deleteAllInBatch();

        seatRepository.deleteAllInBatch();

        roomRepository.deleteAllInBatch();

        cinemaRepository.deleteAllInBatch();
    }

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId, UUID paymentId) {}

    private record ConcurrentOutcome(
            BookingConfirmedConsumerService.Status status, Throwable failure) {

        private static ConcurrentOutcome success(BookingConfirmedConsumerService.Status status) {

            return new ConcurrentOutcome(status, null);
        }

        private static ConcurrentOutcome failure(Throwable failure) {

            return new ConcurrentOutcome(null, failure);
        }
    }
}
