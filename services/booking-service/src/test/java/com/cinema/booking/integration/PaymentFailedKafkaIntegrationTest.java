package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
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
class PaymentFailedKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = "payment-failed";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "booking-payment-failed-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "The payment was declined";

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.booking.kafka.enabled", () -> true);

        registry.add("cinema.booking.kafka.topics.payment-failed", () -> TOPIC);

        registry.add("cinema.booking.kafka.consumer-groups.payment-failed", () -> CONSUMER_GROUP);

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
    void paymentFailedMessageShouldFailBookingAndCreateSeatReleaseOutbox() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message = paymentFailedMessage(context);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.PAYMENT_FAILED)
                                .orElse(false));

        await(() -> processedEventRepository.count() == 1L && findSeatReleaseEvents().size() == 1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        assertThat(booking.getRejectionReason()).isNotEqualTo(FAILURE_MESSAGE);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(booking.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        List<OutboxEventEntity> releaseEvents = findSeatReleaseEvents();

        assertThat(releaseEvents).hasSize(1);

        OutboxEventEntity releaseEvent = releaseEvents.getFirst();

        assertSeatReleaseRequestedOutbox(releaseEvent, context, message);

        JsonNode payload = objectMapper.readTree(releaseEvent.getPayload());

        assertThat(payload.size()).isEqualTo(5);

        assertThat(payload.path("bookingId").asText()).isEqualTo(context.bookingId().toString());

        assertThat(payload.path("showtimeId").asText()).isEqualTo(context.showtimeId().toString());

        assertThat(payload.path("reason").asText()).isEqualTo("PAYMENT_FAILED");

        assertThat(OffsetDateTime.parse(payload.path("requestedAt").asText()))
                .isEqualTo(releaseEvent.getOccurredAt());

        JsonNode seatIds = payload.path("seatIds");

        assertThat(seatIds).hasSize(2);

        assertThat(seatIds.get(0).asText()).isEqualTo(context.firstInventorySeatId().toString());

        assertThat(seatIds.get(1).asText()).isEqualTo(context.secondInventorySeatId().toString());

        /*
         * Provider and payment failure details must not leak into
         * the Inventory compensation command.
         */
        assertThat(payload.has("paymentId")).isFalse();

        assertThat(payload.has("failureCode")).isFalse();

        assertThat(payload.has("message")).isFalse();

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();
    }

    @Test
    void duplicateKafkaDeliveryShouldApplyPaymentFailureOnce() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message = paymentFailedMessage(context);

        publish(context.bookingId(), message);

        publish(context.bookingId(), message);

        await(
                () ->
                        bookingRepository
                                .findById(context.bookingId())
                                .map(booking -> booking.getStatus() == BookingStatus.PAYMENT_FAILED)
                                .orElse(false));

        await(() -> processedEventRepository.count() == 1L && findSeatReleaseEvents().size() == 1);

        awaitStablePaymentFailedState(context.bookingId());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        assertThat(bookingRepository.count()).isEqualTo(1);

        assertThat(bookingSeatRepository.count()).isEqualTo(2);

        List<OutboxEventEntity> releaseEvents = findSeatReleaseEvents();

        assertThat(releaseEvents).hasSize(1);

        assertSeatReleaseRequestedOutbox(releaseEvents.getFirst(), context, message);
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        TestContext context = persistReservedBooking();

        String malformedMessage =
                """
                {
                  "eventType": "payment-failed",
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

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void businessConflictShouldRetryThenReachDeadLetterTopic() throws Exception {

        TestContext context = persistConfirmedBooking();

        OutboxEventMessage message = paymentFailedMessage(context);

        String serializedMessage = objectMapper.writeValueAsString(message);

        kafkaTemplate
                .send(TOPIC, context.bookingId().toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord =
                awaitDeadLetterRecord(context.bookingId().toString());

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(context.bookingId().toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        assertSanitizedDltHeaders(deadLetterRecord.headers());

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        /*
         * Delayed payment-failed must not overwrite a successful
         * Booking confirmation.
         */
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isNotNull();

        assertThat(booking.getRejectionReason()).isNull();

        /*
         * Every retry inserts its marker in the transaction, but
         * the failed transition rolls that marker back.
         */
        assertThat(processedEventRepository.count()).isZero();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext persistReservedBooking() {

        return persistBooking(false);
    }

    private TestContext persistConfirmedBooking() {

        return persistBooking(true);
    }

    private TestContext persistBooking(boolean confirmed) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-failed-kafka-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now.minusMinutes(1));

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        if (confirmed) {
            booking.confirm(now);
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        UUID firstInventorySeatId = UuidGenerator.next();

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(firstInventorySeatId, "STANDARD", FIRST_SEAT_PRICE);

        UUID secondInventorySeatId = UuidGenerator.next();

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(secondInventorySeatId, "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(
                savedBooking.getId(),
                userId,
                showtimeId,
                firstInventorySeatId,
                secondInventorySeatId);
    }

    private OutboxEventMessage paymentFailedMessage(TestContext context) {

        UUID eventId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        paymentId,
                        context.bookingId(),
                        FAILURE_CODE,
                        FAILURE_MESSAGE,
                        failedAt,
                        false);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION,
                failedAt,
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

    private List<OutboxEventEntity> findSeatReleaseEvents() {

        return outboxRepository.findAll().stream()
                .filter(
                        event ->
                                BookingEventContract.SEAT_RELEASE_REQUESTED.equals(
                                        event.getEventType()))
                .toList();
    }

    private void assertSeatReleaseRequestedOutbox(
            OutboxEventEntity event, TestContext context, OutboxEventMessage sourceMessage) {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED);

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

        throw new AssertionError("Asynchronous payment-failed processing did not complete");
    }

    private void awaitStablePaymentFailedState(UUID bookingId) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        int stableObservations = 0;

        while (System.nanoTime() < deadline) {

            boolean stable =
                    processedEventRepository.count() == 1L
                            && bookingRepository.count() == 1L
                            && bookingSeatRepository.count() == 2L
                            && findSeatReleaseEvents().size() == 1
                            && bookingRepository
                                    .findById(bookingId)
                                    .map(
                                            booking ->
                                                    booking.getStatus()
                                                            == BookingStatus.PAYMENT_FAILED)
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

        throw new AssertionError("Payment-failed Kafka consumer state did not become stable");
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord(String expectedKey) {

        Map<String, Object> consumerProperties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "booking-payment-failed-dlt-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {

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

    private record TestContext(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            UUID firstInventorySeatId,
            UUID secondInventorySeatId) {}
}
