package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers(disabledWithoutDocker = true)
class BookingLifecycleReleaseKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CANCELLED_TOPIC = InventoryEventContract.BOOKING_CANCELLED;

    private static final String EXPIRED_TOPIC = InventoryEventContract.BOOKING_EXPIRED;

    private static final String CANCELLED_DLT = CANCELLED_TOPIC + ".dlt";

    private static final String EXPIRED_DLT = EXPIRED_TOPIC + ".dlt";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final OffsetDateTime EVENT_AT =
            OffsetDateTime.of(2026, 9, 16, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT = EVENT_AT.plusMinutes(10);

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.inventory.kafka.enabled", () -> true);

        registry.add("cinema.inventory.kafka.topics.booking-cancelled", () -> CANCELLED_TOPIC);

        registry.add(
                "cinema.inventory.kafka.consumer-groups.booking-cancelled",
                () -> "inventory-booking-cancelled-integration");

        registry.add("cinema.inventory.kafka.topics.booking-expired", () -> EXPIRED_TOPIC);

        registry.add(
                "cinema.inventory.kafka.consumer-groups.booking-expired",
                () -> "inventory-booking-expired-integration");

        registry.add("cinema.inventory.kafka.retry.interval", () -> "10ms");

        registry.add("cinema.inventory.kafka.retry.maximum-retries", () -> 2);

        registry.add("cinema.outbox.enabled", () -> false);

        registry.add("cinema.outbox.producer", () -> "inventory-service");
    }

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @MockitoSpyBean private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private EntityManager entityManager;

    @MockitoSpyBean private BookingCancelledConsumerService bookingCancelledConsumerService;

    @MockitoSpyBean private BookingExpiredConsumerService bookingExpiredConsumerService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        context = createHeldInventory();
    }

    @Test
    void bookingCancelledMessageShouldReleaseHeldSeats() throws Exception {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        publish(CANCELLED_TOPIC, context.bookingId(), message);

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        verify(bookingCancelledConsumerService, org.mockito.Mockito.timeout(10_000).times(1))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        entityManager.clear();

        assertReleased();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CANCELLED_CONSUMER))
                .isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateBookingCancelledDeliveryShouldReleaseOnlyOnce() throws Exception {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        publish(CANCELLED_TOPIC, context.bookingId(), message);

        publish(CANCELLED_TOPIC, context.bookingId(), message);

        verify(bookingCancelledConsumerService, org.mockito.Mockito.timeout(10_000).times(2))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        entityManager.clear();

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void bookingExpiredMessageShouldReleaseHeldSeats() throws Exception {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        publish(EXPIRED_TOPIC, context.bookingId(), message);

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        verify(bookingExpiredConsumerService, org.mockito.Mockito.timeout(10_000).times(1))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        entityManager.clear();

        assertReleased();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), InventoryEventContract.BOOKING_EXPIRED_CONSUMER))
                .isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateBookingExpiredDeliveryShouldReleaseOnlyOnce() throws Exception {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        publish(EXPIRED_TOPIC, context.bookingId(), message);

        publish(EXPIRED_TOPIC, context.bookingId(), message);

        verify(bookingExpiredConsumerService, org.mockito.Mockito.timeout(10_000).times(2))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        entityManager.clear();

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void malformedBookingCancelledMessageShouldGoDirectlyToDlt() throws Exception {

        UUID partitionKey = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "booking-cancelled",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(CANCELLED_TOPIC, partitionKey.toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(CANCELLED_DLT, partitionKey.toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(CANCELLED_DLT);

        assertThat(deadLetterRecord.key()).isEqualTo(partitionKey.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        verify(bookingCancelledConsumerService, never()).handle(anyString(), any());

        entityManager.clear();

        assertHeld();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void malformedBookingExpiredMessageShouldGoDirectlyToDlt() throws Exception {

        UUID partitionKey = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "booking-expired",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(EXPIRED_TOPIC, partitionKey.toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(EXPIRED_DLT, partitionKey.toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(EXPIRED_DLT);

        assertThat(deadLetterRecord.key()).isEqualTo(partitionKey.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        verify(bookingExpiredConsumerService, never()).handle(anyString(), any());

        entityManager.clear();

        assertHeld();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void retryableCancellationFailureShouldRetryThenGoToDlt() throws Exception {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        doThrow(
                        new DataAccessResourceFailureException(
                                "Forced booking-cancelled persistence failure"))
                .when(showSeatRepository)
                .saveAll(anyList());

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(CANCELLED_TOPIC, context.bookingId().toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(CANCELLED_DLT, context.bookingId().toString());

        /*
         * maximum-retries = 2:
         * one initial attempt plus two retry attempts.
         */
        verify(bookingCancelledConsumerService, times(3))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        verify(showSeatRepository, times(3)).saveAll(anyList());

        assertThat(deadLetterRecord.topic()).isEqualTo(CANCELLED_DLT);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        entityManager.clear();

        /*
         * Every failed attempt must roll back both
         * the seat transition and processed-event marker.
         */
        assertHeld();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void retryableExpirationFailureShouldRetryThenGoToDlt() throws Exception {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        doThrow(
                        new DataAccessResourceFailureException(
                                "Forced booking-expired persistence failure"))
                .when(showSeatRepository)
                .saveAll(anyList());

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(EXPIRED_TOPIC, context.bookingId().toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(EXPIRED_DLT, context.bookingId().toString());

        /*
         * maximum-retries = 2:
         * one initial attempt plus two retry attempts.
         */
        verify(bookingExpiredConsumerService, times(3))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        verify(showSeatRepository, times(3)).saveAll(anyList());

        assertThat(deadLetterRecord.topic()).isEqualTo(EXPIRED_DLT);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        entityManager.clear();

        assertHeld();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private void publish(String topic, UUID bookingId, OutboxEventMessage message)
            throws Exception {

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(topic, bookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);
    }

    private OutboxEventMessage bookingCancelledMessage(UUID eventId) {

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.bookingId(),
                        context.userId(),
                        context.showtimeId(),
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        EVENT_AT);

        return new OutboxEventMessage(
                eventId,
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

    private OutboxEventMessage bookingExpiredMessage(UUID eventId) {

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        context.bookingId(), context.userId(), context.showtimeId(), EVENT_AT);

        return new OutboxEventMessage(
                eventId,
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

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Lifecycle Kafka Test",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Lifecycle Kafka Room", RoomType.STANDARD));

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

        ShowSeat first = new ShowSeat(savedShowtime, h7, STANDARD_PRICE);

        first.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        ShowSeat second = new ShowSeat(savedShowtime, h8, VIP_PRICE);

        second.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        showSeatRepository.saveAllAndFlush(List.of(first, second));

        entityManager.clear();

        return new TestContext(bookingId, userId, savedShowtime.getId());
    }

    private boolean allSeatsAvailable() {

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        return showSeats.size() == 2
                && showSeats.stream()
                        .allMatch(showSeat -> showSeat.getStatus() == ShowSeatStatus.AVAILABLE);
    }

    private void assertReleased() {

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });
    }

    private void assertHeld() {

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats)
                .hasSize(2)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId())
                                    .isEqualTo(context.bookingId());

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
                        });
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord(
            String deadLetterTopic, String expectedKey) {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "inventory-lifecycle-dlt-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(deadLetterTopic));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

            while (System.nanoTime() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));

                for (ConsumerRecord<String, String> record : records) {

                    if (expectedKey.equals(record.key())) {

                        return record;
                    }
                }
            }
        }

        throw new AssertionError("Dead-letter record was not published for key " + expectedKey);
    }

    private void assertSanitizedDeadLetterHeaders(Headers headers) {

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();
    }

    private void await(BooleanSupplier condition) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

        while (System.nanoTime() < deadline) {

            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Condition was not satisfied before timeout");
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
