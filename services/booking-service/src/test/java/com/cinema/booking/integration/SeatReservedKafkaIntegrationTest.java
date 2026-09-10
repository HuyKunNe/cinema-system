package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.annotation.IntegrationTest;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
class SeatReservedKafkaIntegrationTest {

    private static final String TOPIC = "seat-reserved";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "booking-seat-reserved-integration";

    private static final String MYSQL_IMAGE = "mysql:8.4.0";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(MYSQL_IMAGE)
                    .withDatabaseName("booking_kafka_test")
                    .withUsername("cinema")
                    .withPassword("cinema");

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);

        registry.add("spring.datasource.username", MYSQL::getUsername);

        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.booking.kafka.enabled", () -> true);

        registry.add("cinema.booking.kafka.topics.seat-reserved", () -> TOPIC);

        registry.add("cinema.booking.kafka.consumer-groups.seat-reserved", () -> CONSUMER_GROUP);

        registry.add("cinema.booking.kafka.retry.interval", () -> "10ms");

        registry.add("cinema.booking.kafka.retry.maximum-retries", () -> 2);

        registry.add("cinema.outbox.enabled", () -> false);

        registry.add("cinema.outbox.producer", () -> "booking-service");
    }

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {

        cleanDatabase();
    }

    @Test
    void seatReservedMessageShouldCompleteSnapshotsAndReserveBooking() throws Exception {

        TestContext context = createPendingBooking();

        OutboxEventMessage message = reservedMessage(context);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.RESERVED)
                                .orElse(false));

        await(() -> processedEventRepository.count() == 1L);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("210000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");

        assertThat(bookingSeats)
                .hasSize(2)
                .allSatisfy(bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isTrue());

        BookingSeat h7 = bookingSeats.get(0);

        BookingSeat h8 = bookingSeats.get(1);

        assertThat(h7.getSeatNumber()).isEqualTo("H7");

        assertThat(h7.getInventorySeatId()).isEqualTo(context.h7InventorySeatId());

        assertThat(h7.getSeatType()).isEqualTo("STANDARD");

        assertThat(h7.getPrice()).isEqualByComparingTo("90000.00");

        assertThat(h8.getSeatNumber()).isEqualTo("H8");

        assertThat(h8.getInventorySeatId()).isEqualTo(context.h8InventorySeatId());

        assertThat(h8.getSeatType()).isEqualTo("VIP");

        assertThat(h8.getPrice()).isEqualByComparingTo("120000.00");

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType())
                                    .isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

                            assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

                            assertThat(event.getPartitionKey())
                                    .isEqualTo(context.bookingId().toString());

                            assertThat(event.getCorrelationId()).isEqualTo(message.correlationId());

                            assertThat(event.getCausationId()).isEqualTo(message.eventId());
                        });
    }

    @Test
    void duplicateKafkaDeliveryShouldApplyReservationOnce() throws Exception {

        TestContext context = createPendingBooking();

        OutboxEventMessage message = reservedMessage(context);

        publish(context.bookingId(), message);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.RESERVED)
                                .orElse(false));

        await(() -> processedEventRepository.count() == 1L);

        /*
         * Wait for the second record to pass through the same consumer
         * group before asserting stable database counts.
         */
        awaitStableState(context.bookingId());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("210000.00");

        assertThat(bookingSeats)
                .hasSize(2)
                .allSatisfy(bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isTrue());

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(bookingRepository.count()).isEqualTo(1);

        assertThat(bookingSeatRepository.count()).isEqualTo(2);

        assertThat(outboxRepository.findAll())
                .singleElement()
                .extracting(OutboxEventEntity::getEventType)
                .isEqualTo(BookingEventContract.PAYMENT_REQUESTED);
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        TestContext context = createPendingBooking();

        String malformedMessage =
                """
                {
                  "eventType": "seat-reserved",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(TOPIC, context.bookingId().toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord = awaitDeadLetterRecord();

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        Headers headers = deadLetterRecord.headers();

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(booking.getTotalAmount()).isNull();

        assertThat(booking.getCurrency()).isNull();

        assertThat(bookingSeats)
                .allSatisfy(
                        bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isFalse());

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private void publish(UUID bookingId, OutboxEventMessage message) throws Exception {

        String serialized = objectMapper.writeValueAsString(message);

        kafkaTemplate.send(TOPIC, bookingId.toString(), serialized).get(10, TimeUnit.SECONDS);
    }

    private OutboxEventMessage reservedMessage(TestContext context) {

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        context.h7InventorySeatId(),
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("90000.00")),
                                new ReservedSeatPayload(
                                        context.h8InventorySeatId(),
                                        "H8",
                                        "VIP",
                                        new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                "seat-reserved",
                "1",
                NOW,
                "inventory-service",
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private TestContext createPendingBooking() {

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        showtimeId,
                        "kafka-reservation-" + UuidGenerator.next(),
                        "a".repeat(64),
                        EXPIRES_AT,
                        NOW);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAllAndFlush(
                List.of(
                        new BookingSeat(savedBooking.getId(), showtimeId, "H7"),
                        new BookingSeat(savedBooking.getId(), showtimeId, "H8")));

        return new TestContext(
                savedBooking.getId(), showtimeId, UuidGenerator.next(), UuidGenerator.next());
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

    private void awaitStableState(UUID bookingId) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        int stableObservations = 0;

        while (System.nanoTime() < deadline) {

            boolean stable =
                    processedEventRepository.count() == 1L
                            && bookingRepository.count() == 1L
                            && bookingSeatRepository.count() == 2L
                            && bookingRepository
                                    .findById(bookingId)
                                    .map(booking -> booking.getStatus() == BookingStatus.RESERVED)
                                    .orElse(false);

            if (stable) {
                stableObservations++;

                if (stableObservations >= 5) {
                    return;
                }
            } else {
                stableObservations = 0;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Kafka consumer state did not become stable");
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord() {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "booking-dlt-verification-" + UUID.randomUUID(),
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

                    return record;
                }
            }
        }

        throw new AssertionError("Dead-letter record was not published");
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }

    private record TestContext(
            UUID bookingId, UUID showtimeId, UUID h7InventorySeatId, UUID h8InventorySeatId) {}
}
