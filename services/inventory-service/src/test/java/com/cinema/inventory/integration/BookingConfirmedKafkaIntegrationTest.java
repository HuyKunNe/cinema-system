package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class BookingConfirmedKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = InventoryEventContract.BOOKING_CONFIRMED;

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "inventory-booking-confirmed-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final OffsetDateTime CONFIRMED_AT =
            OffsetDateTime.of(2026, 9, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT = CONFIRMED_AT.plusMinutes(10);

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.inventory.kafka.enabled", () -> true);

        registry.add("cinema.inventory.kafka.topics.booking-confirmed", () -> TOPIC);

        registry.add(
                "cinema.inventory.kafka.consumer-groups.booking-confirmed", () -> CONSUMER_GROUP);

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

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private EntityManager entityManager;

    @MockitoSpyBean private BookingConfirmedConsumerService bookingConfirmedConsumerService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        context = createHeldInventory();
    }

    @Test
    void bookingConfirmedMessageShouldBookHeldSeats() throws Exception {

        OutboxEventMessage message =
                bookingConfirmedMessage(UuidGenerator.next(), context.bookingId());

        publish(context.bookingId(), message);

        await(
                () ->
                        showSeatRepository
                                .findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId())
                                .stream()
                                .allMatch(
                                        showSeat -> showSeat.getStatus() == ShowSeatStatus.BOOKED));

        await(() -> processedEventRepository.count() == 1L);

        verify(bookingConfirmedConsumerService, timeout(10_000).times(1))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        entityManager.clear();

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        /*
         * booking-confirmed is terminal for Inventory.
         * Inventory does not publish another event for this transition.
         */
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateKafkaDeliveryShouldBookOnlyOnce() throws Exception {

        OutboxEventMessage message =
                bookingConfirmedMessage(UuidGenerator.next(), context.bookingId());

        publish(context.bookingId(), message);

        publish(context.bookingId(), message);

        verify(bookingConfirmedConsumerService, timeout(10_000).times(2))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        await(() -> processedEventRepository.count() == 1L);

        entityManager.clear();

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats)
                .hasSize(2)
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
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        UUID partitionKey = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "booking-confirmed",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(TOPIC, partitionKey.toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(partitionKey.toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(partitionKey.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        /*
         * Reader failure occurs before the transactional consumer is called.
         * ValidationException is configured as non-retryable.
         */
        verify(bookingConfirmedConsumerService, never()).handle(anyString(), any());

        entityManager.clear();

        assertHeldByOriginalBooking();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void incompatibleBusinessStateShouldRetryThenBeSentToDeadLetterTopic() throws Exception {

        UUID conflictingBookingId = UuidGenerator.next();

        OutboxEventMessage message =
                bookingConfirmedMessage(UuidGenerator.next(), conflictingBookingId);

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(TOPIC, conflictingBookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(conflictingBookingId.toString());

        /*
         * maximum-retries = 2 means:
         * one initial attempt plus two retry attempts.
         */
        verify(bookingConfirmedConsumerService, times(3))
                .handle(eq(conflictingBookingId.toString()), any(OutboxEventMessage.class));

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(conflictingBookingId.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        entityManager.clear();

        assertHeldByOriginalBooking();

        /*
         * The processed-event insertion must roll back on every rejected
         * attempt, so this event remains safe for controlled replay.
         */
        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private void publish(UUID bookingId, OutboxEventMessage message) throws Exception {

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(TOPIC, bookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);
    }

    private OutboxEventMessage bookingConfirmedMessage(UUID eventId, UUID bookingId) {

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        bookingId,
                        context.userId(),
                        context.showtimeId(),
                        context.paymentId(),
                        List.of(
                                new ConfirmedSeatPayload(
                                        "H7", SeatType.STANDARD.name(), STANDARD_PRICE),
                                new ConfirmedSeatPayload("H8", SeatType.VIP.name(), VIP_PRICE)),
                        TOTAL_AMOUNT,
                        InventoryEventContract.CURRENCY_VND,
                        CONFIRMED_AT);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                CONFIRMED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Confirmed Kafka Test",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Booking Confirmed Room", RoomType.STANDARD));

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

    private void assertHeldByOriginalBooking() {

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

    private void assertSanitizedDeadLetterHeaders(Headers headers) {

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord(String expectedKey) {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "inventory-booking-confirmed-dlt-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(DEAD_LETTER_TOPIC));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

            while (System.nanoTime() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));

                for (ConsumerRecord<String, String> record : records) {

                    /*
                     * The class contains multiple DLT scenarios. Filtering by
                     * key prevents a fresh consumer from returning a record
                     * created by an earlier test.
                     */
                    if (expectedKey.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        throw new AssertionError("Dead-letter record was not published for key " + expectedKey);
    }

    private void await(BooleanSupplier condition) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

        while (System.nanoTime() < deadline) {

            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Asynchronous Kafka processing did not complete");
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
