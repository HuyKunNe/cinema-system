package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.service.OutboxService;
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
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Import(SeatReleaseRequestedConsumerRollbackIntegrationTest.FixedClockConfiguration.class)
class SeatReleaseRequestedConsumerRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-15T10:01:00Z");

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-15T10:10:00Z");

    @Autowired private SeatReleaseRequestedConsumerService consumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @MockitoSpyBean private OutboxService outboxService;

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
    void outboxPersistenceFailureShouldRollbackSeatsAndProcessedMarker() {

        OutboxEventMessage message = releaseMessage();

        doThrow(new DataAccessResourceFailureException("Forced seat-released Outbox failure"))
                .when(outboxService)
                .save(any(OutboxEventEntity.class));

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("Forced seat-released Outbox failure");

        entityManager.clear();

        List<ShowSeat> reloadedSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(reloadedSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        /*
         * Consumer đã mutate entity thành AVAILABLE trước khi gọi Outbox,
         * nhưng transaction failure phải khôi phục database về HELD.
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
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Seat Release Rollback Test",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Release Rollback Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        OffsetDateTime currentTime = OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        currentTime.plusDays(1),
                        currentTime.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat firstShowSeat = new ShowSeat(savedShowtime, h7, new BigDecimal("90000.00"));

        firstShowSeat.hold(bookingId, HOLD_EXPIRES_AT, REQUESTED_AT.minusMinutes(1));

        ShowSeat secondShowSeat = new ShowSeat(savedShowtime, h8, new BigDecimal("120000.00"));

        secondShowSeat.hold(bookingId, HOLD_EXPIRES_AT, REQUESTED_AT.minusMinutes(1));

        List<ShowSeat> savedShowSeats =
                showSeatRepository.saveAllAndFlush(List.of(firstShowSeat, secondShowSeat));

        List<UUID> showSeatIds =
                savedShowSeats.stream()
                        .map(ShowSeat::getId)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        entityManager.clear();

        return new TestContext(bookingId, savedShowtime.getId(), showSeatIds);
    }

    private OutboxEventMessage releaseMessage() {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(context.showSeatIds().get(1), context.showSeatIds().get(0)),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                REQUESTED_AT,
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

    private record TestContext(UUID bookingId, UUID showtimeId, List<UUID> showSeatIds) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedSeatReleaseRollbackClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
