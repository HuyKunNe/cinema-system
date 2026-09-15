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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class BookingConfirmedConsumerRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final OffsetDateTime CONFIRMED_AT =
            OffsetDateTime.of(2026, 9, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT = CONFIRMED_AT.plusMinutes(10);

    @Autowired private BookingConfirmedConsumerService consumerService;

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
    void showSeatPersistenceFailureShouldRollbackSeatsAndProcessedMarker() {

        OutboxEventMessage message = bookingConfirmedMessage();

        doAnswer(
                        invocation -> {
                            List<ShowSeat> mutatedSeats = invocation.getArgument(0);

                            /*
                             * Prove that the failure happens after the domain
                             * transition has changed every entity in memory.
                             */
                            assertThat(mutatedSeats)
                                    .allSatisfy(
                                            showSeat -> {
                                                assertThat(showSeat.getStatus())
                                                        .isEqualTo(ShowSeatStatus.BOOKED);

                                                assertThat(showSeat.getHeldByBookingId()).isNull();

                                                assertThat(showSeat.getHoldExpiresAt()).isNull();
                                            });

                            throw new DataAccessResourceFailureException(
                                    "Forced booking-confirmed persistence failure");
                        })
                .when(showSeatRepository)
                .saveAll(anyList());

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("Forced booking-confirmed persistence failure");

        entityManager.clear();

        List<ShowSeat> reloadedSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(reloadedSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        /*
         * Database state must be restored even though the managed entities
         * had already transitioned to BOOKED before repository failure.
         */
        assertThat(reloadedSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId())
                                    .isEqualTo(context.bookingId());

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
                        });

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Confirmation Rollback",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(new Room(cinema, "Rollback Room", RoomType.STANDARD));

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

        firstShowSeat.hold(bookingId, HOLD_EXPIRES_AT, CONFIRMED_AT.minusMinutes(1));

        ShowSeat secondShowSeat = new ShowSeat(savedShowtime, h8, VIP_PRICE);

        secondShowSeat.hold(bookingId, HOLD_EXPIRES_AT, CONFIRMED_AT.minusMinutes(1));

        showSeatRepository.saveAllAndFlush(List.of(firstShowSeat, secondShowSeat));

        entityManager.clear();

        return new TestContext(bookingId, userId, savedShowtime.getId(), paymentId);
    }

    private OutboxEventMessage bookingConfirmedMessage() {

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
                UuidGenerator.next(),
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
}
