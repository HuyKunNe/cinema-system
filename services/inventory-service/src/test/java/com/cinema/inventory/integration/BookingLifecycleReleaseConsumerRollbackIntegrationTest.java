package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

import com.cinema.common.core.id.UuidGenerator;
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
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.BookingCancelledConsumerService;
import com.cinema.inventory.service.BookingExpiredConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class BookingLifecycleReleaseConsumerRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime EVENT_AT =
            OffsetDateTime.of(2026, 9, 16, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT = EVENT_AT.plusMinutes(10);

    @Autowired private BookingCancelledConsumerService bookingCancelledConsumerService;

    @Autowired private BookingExpiredConsumerService bookingExpiredConsumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @MockitoSpyBean private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        context = createHeldInventory();
    }

    @AfterEach
    void tearDown() {

        cleanDatabase();
    }

    @Test
    void cancellationPersistenceFailureShouldRollbackSeatsAndProcessedMarker() {

        OutboxEventMessage message = bookingCancelledMessage();

        doAnswer(
                        invocation -> {
                            List<ShowSeat> mutatedSeats = invocation.getArgument(0);

                            assertThat(mutatedSeats)
                                    .allSatisfy(
                                            showSeat -> {
                                                assertThat(showSeat.getStatus())
                                                        .isEqualTo(ShowSeatStatus.AVAILABLE);

                                                assertThat(showSeat.getHeldByBookingId()).isNull();

                                                assertThat(showSeat.getHoldExpiresAt()).isNull();
                                            });

                            throw new DataAccessResourceFailureException(
                                    "Forced booking-cancelled persistence failure");
                        })
                .when(showSeatRepository)
                .saveAll(anyList());

        assertThatThrownBy(
                        () ->
                                bookingCancelledConsumerService.handle(
                                        context.bookingId().toString(), message))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("Forced booking-cancelled persistence failure");

        entityManager.clear();

        assertHeld();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CANCELLED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void expirationPersistenceFailureShouldRollbackSeatsAndProcessedMarker() {

        OutboxEventMessage message = bookingExpiredMessage();

        doAnswer(
                        invocation -> {
                            List<ShowSeat> mutatedSeats = invocation.getArgument(0);

                            assertThat(mutatedSeats)
                                    .allSatisfy(
                                            showSeat -> {
                                                assertThat(showSeat.getStatus())
                                                        .isEqualTo(ShowSeatStatus.AVAILABLE);

                                                assertThat(showSeat.getHeldByBookingId()).isNull();

                                                assertThat(showSeat.getHoldExpiresAt()).isNull();
                                            });

                            throw new DataAccessResourceFailureException(
                                    "Forced booking-expired persistence failure");
                        })
                .when(showSeatRepository)
                .saveAll(anyList());

        assertThatThrownBy(
                        () ->
                                bookingExpiredConsumerService.handle(
                                        context.bookingId().toString(), message))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("Forced booking-expired persistence failure");

        entityManager.clear();

        assertHeld();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), InventoryEventContract.BOOKING_EXPIRED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Lifecycle Rollback",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Lifecycle Rollback Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        EVENT_AT.plusDays(1),
                        EVENT_AT.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat first = new ShowSeat(savedShowtime, h7, new BigDecimal("90000.00"));

        first.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        ShowSeat second = new ShowSeat(savedShowtime, h8, new BigDecimal("120000.00"));

        second.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        showSeatRepository.saveAllAndFlush(List.of(first, second));

        entityManager.clear();

        return new TestContext(bookingId, userId, savedShowtime.getId());
    }

    private OutboxEventMessage bookingCancelledMessage() {

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.bookingId(),
                        context.userId(),
                        context.showtimeId(),
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        EVENT_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CANCELLED,
                InventoryEventContract.BOOKING_CANCELLED_VERSION,
                EVENT_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage bookingExpiredMessage() {

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        context.bookingId(), context.userId(), context.showtimeId(), EVENT_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_EXPIRED,
                InventoryEventContract.BOOKING_EXPIRED_VERSION,
                EVENT_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void assertHeld() {

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId())
                                    .isEqualTo(context.bookingId());

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
                        });
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

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId) {}
}
