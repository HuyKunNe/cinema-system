package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
class PaymentSucceededKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = "payment-succeeded";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "booking-payment-succeeded-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.booking.kafka.enabled", () -> true);

        registry.add("cinema.booking.kafka.topics.payment-succeeded", () -> TOPIC);

        registry.add(
                "cinema.booking.kafka.consumer-groups.payment-succeeded", () -> CONSUMER_GROUP);

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
    void paymentSucceededMessageShouldConfirmBookingAndCreateOutbox() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message = paymentSucceededMessage(context);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                                .orElse(false));

        await(() -> processedEventRepository.count() == 1L && outboxRepository.count() == 1L);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isNotNull();

        assertThat(booking.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        List<OutboxEventEntity> confirmedEvents = findBookingConfirmedEvents();

        assertThat(confirmedEvents).hasSize(1);

        OutboxEventEntity confirmedEvent = confirmedEvents.getFirst();

        assertBookingConfirmedOutbox(confirmedEvent, context, message);

        JsonNode payload = objectMapper.readTree(confirmedEvent.getPayload());

        assertThat(payload.size()).isEqualTo(8);

        assertThat(payload.path("bookingId").asText()).isEqualTo(context.bookingId().toString());

        assertThat(payload.path("userId").asText()).isEqualTo(context.userId().toString());

        assertThat(payload.path("showtimeId").asText()).isEqualTo(context.showtimeId().toString());

        assertThat(payload.path("paymentId").asText()).isEqualTo(message.aggregateId().toString());

        assertThat(payload.path("totalAmount").decimalValue()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(payload.path("currency").asText()).isEqualTo(CURRENCY);

        assertThat(payload.path("confirmedAt").asText()).isNotBlank();

        assertThat(payload.path("seats")).hasSize(2);

        assertThat(payload.path("seats").get(0).path("seatNumber").asText()).isEqualTo("H7");

        assertThat(payload.path("seats").get(0).path("seatType").asText()).isEqualTo("STANDARD");

        assertThat(payload.path("seats").get(0).path("price").decimalValue())
                .isEqualByComparingTo(FIRST_SEAT_PRICE);

        assertThat(payload.path("seats").get(1).path("seatNumber").asText()).isEqualTo("H8");

        assertThat(payload.path("seats").get(1).path("seatType").asText()).isEqualTo("VIP");

        assertThat(payload.path("seats").get(1).path("price").decimalValue())
                .isEqualByComparingTo(SECOND_SEAT_PRICE);

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();
    }

    @Test
    void duplicateKafkaDeliveryShouldConfirmOnlyOnce() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message = paymentSucceededMessage(context);

        publish(context.bookingId(), message);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                                .orElse(false));

        await(
                () ->
                        processedEventRepository.count() == 1L
                                && findBookingConfirmedEvents().size() == 1);

        awaitStableConfirmedState(context.bookingId());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isNotNull();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        assertThat(bookingRepository.count()).isEqualTo(1);

        assertThat(bookingSeatRepository.count()).isEqualTo(2);

        List<OutboxEventEntity> confirmedEvents = findBookingConfirmedEvents();

        assertThat(confirmedEvents).hasSize(1);

        assertBookingConfirmedOutbox(confirmedEvents.getFirst(), context, message);
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        TestContext context = persistReservedBooking();

        String malformedMessage =
                """
                {
                  "eventType": "payment-succeeded",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(TOPIC, context.bookingId().toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(context.bookingId().toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        assertSanitizedDltHeaders(deadLetterRecord.headers());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void businessConflictShouldRetryThenReachDeadLetterTopic() throws Exception {

        TestContext context = persistCancelledBooking();

        OutboxEventMessage message = paymentSucceededMessage(context);

        publish(context.bookingId(), message);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(context.bookingId().toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(objectMapper.writeValueAsString(message));

        assertSanitizedDltHeaders(deadLetterRecord.headers());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isNotNull();

        assertThat(booking.getConfirmedAt()).isNull();

        /*
         * Mỗi retry insert marker trong cùng transaction với transition.
         * Transition thất bại nên marker phải rollback ở mọi attempt.
         */
        assertThat(processedEventRepository.count()).isZero();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext persistReservedBooking() {

        return persistBooking(false);
    }

    private TestContext persistCancelledBooking() {

        return persistBooking(true);
    }

    private TestContext persistBooking(boolean cancelled) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-success-kafka-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now.minusMinutes(1));

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        if (cancelled) {
            booking.cancel(now);
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(UuidGenerator.next(), "STANDARD", FIRST_SEAT_PRICE);

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(UuidGenerator.next(), "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(savedBooking.getId(), userId, showtimeId);
    }

    private OutboxEventMessage paymentSucceededMessage(TestContext context) {

        UUID eventId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        OffsetDateTime paidAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        paymentId,
                        context.bookingId(),
                        TOTAL_AMOUNT,
                        CURRENCY,
                        "MOMO",
                        "momo-" + paymentId,
                        paidAt);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                paidAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void publish(UUID bookingId, OutboxEventMessage message) throws Exception {

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(TOPIC, bookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);
    }

    private List<OutboxEventEntity> findBookingConfirmedEvents() {

        return outboxRepository.findAll().stream()
                .filter(
                        event ->
                                BookingEventContract.BOOKING_CONFIRMED.equals(event.getEventType()))
                .toList();
    }

    private void assertBookingConfirmedOutbox(
            OutboxEventEntity event, TestContext context, OutboxEventMessage sourceMessage) {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.BOOKING_CONFIRMED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(sourceMessage.eventId());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getOccurredAt()).isNotNull();

        assertThat(event.getNextAttemptAt()).isEqualTo(event.getOccurredAt());

        assertThat(event.getCreatedAt()).isEqualTo(event.getOccurredAt());
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

    private void awaitStableConfirmedState(UUID bookingId) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        int stableObservations = 0;

        while (System.nanoTime() < deadline) {

            boolean stable =
                    processedEventRepository.count() == 1L
                            && bookingRepository.count() == 1L
                            && bookingSeatRepository.count() == 2L
                            && findBookingConfirmedEvents().size() == 1
                            && bookingRepository
                                    .findById(bookingId)
                                    .map(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
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

    private ConsumerRecord<String, String> awaitDeadLetterRecord(String expectedKey) {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "booking-payment-dlt-" + UUID.randomUUID(),
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

                    if (expectedKey.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        throw new AssertionError("Dead-letter record was not published for key " + expectedKey);
    }

    private void assertSanitizedDltHeaders(Headers headers) {

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        bookingSeatRepository.deleteAllInBatch();

        bookingRepository.deleteAllInBatch();
    }

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId) {}
}
