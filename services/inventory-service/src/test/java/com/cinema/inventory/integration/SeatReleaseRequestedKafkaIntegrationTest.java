package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
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
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@Import(SeatReleaseRequestedKafkaIntegrationTest.FixedClockConfiguration.class)
class SeatReleaseRequestedKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = InventoryEventContract.SEAT_RELEASE_REQUESTED;

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "inventory-seat-release-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-15T10:01:00Z");

    private static final OffsetDateTime RELEASED_AT =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-15T10:10:00Z");

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.inventory.kafka.enabled", () -> true);

        registry.add("cinema.inventory.kafka.topics.seat-release-requested", () -> TOPIC);

        registry.add(
                "cinema.inventory.kafka.consumer-groups." + "seat-release-requested",
                () -> CONSUMER_GROUP);

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

    @MockitoSpyBean private SeatReleaseRequestedConsumerService consumerService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        context = createHeldInventory();
    }

    @Test
    void seatReleaseRequestedMessageShouldReleaseSeatsAndCreateOutbox() throws Exception {

        OutboxEventMessage message = releaseMessage(UuidGenerator.next(), context.bookingId());

        publish(context.bookingId(), message);

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        await(() -> outboxRepository.count() == 1L);

        verify(consumerService, org.mockito.Mockito.timeout(10_000).times(1))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        entityManager.clear();

        List<ShowSeat> showSeats = loadShowSeats();

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(outboxEvent -> assertCanonicalOutbox(outboxEvent, message));
    }

    @Test
    void duplicateKafkaDeliveryShouldReleaseAndCreateOutboxOnlyOnce() throws Exception {

        OutboxEventMessage message = releaseMessage(UuidGenerator.next(), context.bookingId());

        publish(context.bookingId(), message);

        publish(context.bookingId(), message);

        verify(consumerService, org.mockito.Mockito.timeout(10_000).times(2))
                .handle(eq(context.bookingId().toString()), any(OutboxEventMessage.class));

        await(this::allSeatsAvailable);

        await(() -> processedEventRepository.count() == 1L);

        await(() -> outboxRepository.count() == 1L);

        entityManager.clear();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        UUID partitionKey = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "seat-release-requested",
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
         * Message reader fails before the transactional service is called.
         * ValidationException is non-retryable.
         */
        verify(consumerService, never()).handle(anyString(), any());

        entityManager.clear();

        assertStillHeldByOriginalBooking();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void incompatibleBusinessStateShouldRetryThenBeSentToDeadLetterTopic() throws Exception {

        UUID conflictingBookingId = UuidGenerator.next();

        OutboxEventMessage message = releaseMessage(UuidGenerator.next(), conflictingBookingId);

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(TOPIC, conflictingBookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(conflictingBookingId.toString());

        /*
         * maximum-retries = 2:
         * one initial invocation plus two retries.
         */
        verify(consumerService, times(3))
                .handle(eq(conflictingBookingId.toString()), any(OutboxEventMessage.class));

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(conflictingBookingId.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        assertSanitizedDeadLetterHeaders(deadLetterRecord.headers());

        entityManager.clear();

        assertStillHeldByOriginalBooking();

        /*
         * Each failed attempt must roll back its processed-event marker.
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

    private OutboxEventMessage releaseMessage(UUID eventId, UUID bookingId) {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        context.showtimeId(),
                        List.of(context.showSeatIds().get(1), context.showSeatIds().get(0)),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                REQUESTED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Seat Release Kafka Test", "123 Main Street", "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Release Kafka Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        RELEASED_AT.plusDays(1),
                        RELEASED_AT.plusDays(1).plusHours(2));

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

    private void assertCanonicalOutbox(
            OutboxEventEntity outboxEvent, OutboxEventMessage sourceMessage) {

        assertThat(outboxEvent.getEventType()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(outboxEvent.getEventVersion())
                .isEqualTo(InventoryEventContract.SEAT_RELEASED_VERSION);

        assertThat(outboxEvent.getTopic()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(outboxEvent.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(outboxEvent.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(outboxEvent.getOccurredAt()).isEqualTo(RELEASED_AT);

        assertThat(outboxEvent.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(outboxEvent.getCausationId()).isEqualTo(sourceMessage.eventId());

        try {
            JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());

            assertThat(payload.path("bookingId").asText())
                    .isEqualTo(context.bookingId().toString());

            assertThat(payload.path("showtimeId").asText())
                    .isEqualTo(context.showtimeId().toString());

            assertThat(payload.path("reason").asText())
                    .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

            assertThat(OffsetDateTime.parse(payload.path("releasedAt").asText()))
                    .isEqualTo(RELEASED_AT);

            assertThat(payload.path("releasedSeatIds"))
                    .extracting(JsonNode::asText)
                    .containsExactly(
                            context.showSeatIds().get(0).toString(),
                            context.showSeatIds().get(1).toString());

        } catch (Exception exception) {
            throw new AssertionError("Unable to verify seat-released payload", exception);
        }
    }

    private boolean allSeatsAvailable() {

        List<ShowSeat> showSeats = loadShowSeats();

        return showSeats.size() == 2
                && showSeats.stream()
                        .allMatch(showSeat -> showSeat.getStatus() == ShowSeatStatus.AVAILABLE);
    }

    private List<ShowSeat> loadShowSeats() {

        return showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());
    }

    private void assertStillHeldByOriginalBooking() {

        assertThat(loadShowSeats())
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
                        "inventory-seat-release-dlt-" + UUID.randomUUID(),
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
                     * This class has two DLT scenarios. A new consumer using
                     * earliest may also see a record from the previous test.
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

    private record TestContext(UUID bookingId, UUID showtimeId, List<UUID> showSeatIds) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedSeatReleaseKafkaClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
