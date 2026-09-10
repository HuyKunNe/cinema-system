package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.annotation.IntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ProcessedEventRepository;
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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentRequestedKafkaIntegrationTest {

    private static final String TOPIC = "payment-requested";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "payment-request-processing-integration";

    private static final String MYSQL_IMAGE = "mysql:8.4.0";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final OffsetDateTime REQUESTED_AT =
            OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

    private static final OffsetDateTime HOLD_EXPIRES_AT = REQUESTED_AT.plusMinutes(30);

    private static final BigDecimal AMOUNT = new BigDecimal("210000.00");

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(MYSQL_IMAGE)
                    .withDatabaseName("payment_kafka_test")
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

        registry.add("cinema.payment.provider", () -> "MOCK");

        registry.add("cinema.payment.kafka.enabled", () -> true);

        registry.add("cinema.payment.kafka.topics.payment-requested", () -> TOPIC);

        registry.add(
                "cinema.payment.kafka.consumer-groups.payment-requested", () -> CONSUMER_GROUP);

        registry.add("cinema.payment.kafka.retry.interval", () -> "10ms");

        registry.add("cinema.payment.kafka.retry.maximum-retries", () -> 2);

        registry.add("cinema.outbox.enabled", () -> false);

        registry.add("cinema.outbox.producer", () -> "payment-service");
    }

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {

        cleanDatabase();
    }

    @Test
    void paymentRequestedMessageShouldCreatePaymentAndReadyCharge() throws Exception {

        TestContext context = newContext();

        OutboxEventMessage message = paymentRequestedMessage(context);

        publish(context.bookingId(), message);

        await(() -> paymentRepository.findBySourceEventId(context.eventId()).isPresent());

        await(() -> processedEventRepository.count() == 1L);

        entityManager.clear();

        Payment payment = paymentRepository.findBySourceEventId(context.eventId()).orElseThrow();

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertThat(payment.getBookingId()).isEqualTo(context.bookingId());

        assertThat(payment.getUserId()).isEqualTo(context.userId());

        assertThat(payment.getPaymentAttempt()).isEqualTo(1);

        assertThat(payment.getAmount()).isEqualByComparingTo(AMOUNT);

        assertThat(payment.getCurrency()).isEqualTo("VND");

        assertThat(payment.getProvider()).isEqualTo("MOCK");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);

        assertThat(payment.getHoldExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);

        assertThat(payment.getRequestedAt()).isEqualTo(REQUESTED_AT);

        assertThat(payment.getSourceEventId()).isEqualTo(context.eventId());

        assertThat(payment.getCorrelationId()).isEqualTo(context.correlationId());

        assertThat(transactions).hasSize(1);

        PaymentTransaction transaction = transactions.getFirst();

        assertThat(transaction.getPaymentId()).isEqualTo(payment.getId());

        assertThat(transaction.getProvider()).isEqualTo("MOCK");

        assertThat(transaction.getTransactionType()).isEqualTo(PaymentTransactionType.CHARGE);

        assertThat(transaction.getAttemptNumber()).isEqualTo(1);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.READY);

        assertThat(transaction.getAmount()).isEqualByComparingTo(AMOUNT);

        assertThat(transaction.getCurrency()).isEqualTo("VND");

        assertThat(transaction.getIdempotencyKey()).isNotBlank();

        assertThat(transaction.getRequestedAt())
                .isNotNull()
                .isAfterOrEqualTo(payment.getRequestedAt())
                .isBeforeOrEqualTo(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1));

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateKafkaDeliveryShouldCreateOnePaymentAndCharge() throws Exception {

        TestContext context = newContext();

        OutboxEventMessage message = paymentRequestedMessage(context);

        publish(context.bookingId(), message);
        publish(context.bookingId(), message);

        await(() -> paymentRepository.findBySourceEventId(context.eventId()).isPresent());

        await(() -> processedEventRepository.count() == 1L);

        awaitStableState(context);

        entityManager.clear();

        Payment payment = paymentRepository.findBySourceEventId(context.eventId()).orElseThrow();

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);

        assertThat(paymentRepository.count()).isEqualTo(1);

        assertThat(transactions).hasSize(1);

        assertThat(transactionRepository.count()).isEqualTo(1);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        UUID bookingId = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "payment-requested",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate.send(TOPIC, bookingId.toString(), malformedMessage).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord = awaitDeadLetterRecord();

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(bookingId.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        Headers headers = deadLetterRecord.headers();

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();

        assertThat(paymentRepository.count()).isZero();

        assertThat(transactionRepository.count()).isZero();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private void publish(UUID bookingId, OutboxEventMessage message) throws Exception {

        String serialized = objectMapper.writeValueAsString(message);

        kafkaTemplate.send(TOPIC, bookingId.toString(), serialized).get(10, TimeUnit.SECONDS);
    }

    private OutboxEventMessage paymentRequestedMessage(TestContext context) {

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        context.bookingId(),
                        context.userId(),
                        AMOUNT,
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        return new OutboxEventMessage(
                context.eventId(),
                context.bookingId(),
                "BOOKING",
                "payment-requested",
                "1",
                REQUESTED_AT,
                "booking-service",
                context.correlationId(),
                context.causationId(),
                objectMapper.valueToTree(payload));
    }

    private TestContext newContext() {

        return new TestContext(
                UuidGenerator.next(),
                UuidGenerator.next(),
                UuidGenerator.next(),
                UuidGenerator.next(),
                UuidGenerator.next());
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

    private void awaitStableState(TestContext context) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        int stableObservations = 0;

        while (System.nanoTime() < deadline) {

            boolean stable =
                    processedEventRepository.count() == 1L
                            && paymentRepository.count() == 1L
                            && transactionRepository.count() == 1L
                            && paymentRepository.findBySourceEventId(context.eventId()).isPresent();

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
                        "payment-dlt-verification-" + UUID.randomUUID(),
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

        transactionRepository.deleteAll();

        paymentRepository.deleteAll();
    }

    private record TestContext(
            UUID bookingId, UUID userId, UUID eventId, UUID correlationId, UUID causationId) {}
}
