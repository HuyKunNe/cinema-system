package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.config.OutboxConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.publisher.OutboxPublisher;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.PaymentFailedOutboxFactory;
import com.cinema.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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

@Import(OutboxConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
        properties = {
            "spring.cloud.config.enabled=false",
            "eureka.client.enabled=false",
            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.payment.provider-operation.scheduling-enabled=false",
            "cinema.outbox.enabled=false",
            "cinema.outbox.producer=payment-service",
            "cinema.outbox.batch-size=10",
            "cinema.outbox.lease-duration=30s",
            "cinema.outbox.maximum-attempts=5",
            "cinema.outbox.base-retry-delay=10ms",
            "cinema.outbox.maximum-retry-delay=100ms",
            "cinema.outbox.maximum-jitter=0ms"
        })
class PaymentFailedOutboxKafkaIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = PaymentEventContract.PAYMENT_FAILED;

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final BigDecimal AMOUNT = new BigDecimal("180000.00");

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "The payment was declined";

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add(
                "spring.kafka.producer.key-serializer", () -> StringSerializer.class.getName());

        registry.add(
                "spring.kafka.producer.value-serializer", () -> StringSerializer.class.getName());

        registry.add(
                "spring.kafka.consumer.key-deserializer", () -> StringDeserializer.class.getName());

        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> StringDeserializer.class.getName());

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private PaymentFailedOutboxFactory failedOutboxFactory;

    @Autowired private OutboxClaimService claimService;

    @Autowired private OutboxPublisher publisher;

    @Autowired private OutboxAcknowledgementService acknowledgementService;

    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        outboxRepository.deleteAllInBatch();

        paymentRepository.deleteAllInBatch();
    }

    @Test
    void paymentFailedOutboxShouldPublishCanonicalKafkaMessageAndBecomeSent() throws Exception {

        Payment payment = persistFailedPayment();

        OutboxEventEntity persistedEvent = failedOutboxFactory.create(payment);

        outboxRepository.saveAndFlush(persistedEvent);

        assertPendingEvent(persistedEvent, payment);

        List<OutboxEventEntity> claimedEvents = claimService.claimNextBatch();

        assertThat(claimedEvents).hasSize(1);

        OutboxEventEntity claimedEvent = claimedEvents.getFirst();

        assertThat(claimedEvent.getId()).isEqualTo(persistedEvent.getId());

        assertThat(claimedEvent.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        assertThat(claimedEvent.getProcessingOwner()).isNotBlank();

        String processingOwner = claimedEvent.getProcessingOwner();

        publisher.publish(claimedEvent).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> kafkaRecord = awaitPaymentFailedRecord();

        assertKafkaRecord(kafkaRecord, payment, persistedEvent);

        boolean acknowledged =
                acknowledgementService.acknowledgeSuccess(claimedEvent.getId(), processingOwner);

        assertThat(acknowledged).isTrue();

        assertSentState(persistedEvent.getId());
    }

    private Payment persistFailedPayment() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        AMOUNT,
                        "VND",
                        "MOCK",
                        now.plusMinutes(30),
                        now.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderFailure(FAILURE_CODE, FAILURE_MESSAGE, now);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        return paymentRepository.saveAndFlush(payment);
    }

    private static void assertPendingEvent(OutboxEventEntity event, Payment payment) {

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        assertThat(event.getEventVersion()).isEqualTo(PaymentEventContract.PAYMENT_FAILED_VERSION);

        assertThat(event.getTopic()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        assertThat(event.getAggregateId()).isEqualTo(payment.getId());

        assertThat(event.getPartitionKey()).isEqualTo(payment.getBookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(payment.getCorrelationId());

        assertThat(event.getCausationId()).isEqualTo(payment.getSourceEventId());
    }

    private void assertKafkaRecord(
            ConsumerRecord<String, String> kafkaRecord,
            Payment payment,
            OutboxEventEntity persistedEvent)
            throws Exception {

        assertThat(kafkaRecord.topic()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        assertThat(kafkaRecord.key()).isEqualTo(payment.getBookingId().toString());

        OutboxEventMessage message =
                objectMapper.readValue(kafkaRecord.value(), OutboxEventMessage.class);

        assertThat(message.eventId()).isEqualTo(persistedEvent.getId());

        assertThat(message.aggregateId()).isEqualTo(payment.getId());

        assertThat(message.aggregateType()).isEqualTo("PAYMENT");

        assertThat(message.eventType()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        assertThat(message.eventVersion()).isEqualTo(PaymentEventContract.PAYMENT_FAILED_VERSION);

        assertThat(message.producer()).isEqualTo("payment-service");

        assertThat(message.occurredAt()).isEqualTo(payment.getCompletedAt());

        assertThat(message.correlationId()).isEqualTo(payment.getCorrelationId());

        assertThat(message.causationId()).isEqualTo(payment.getSourceEventId());

        assertThat(message.payload().get("paymentId").asText())
                .isEqualTo(payment.getId().toString());

        assertThat(message.payload().get("bookingId").asText())
                .isEqualTo(payment.getBookingId().toString());

        assertThat(message.payload().get("failureCode").asText()).isEqualTo(FAILURE_CODE);

        assertThat(message.payload().get("message").asText()).isEqualTo(FAILURE_MESSAGE);

        assertThat(OffsetDateTime.parse(message.payload().get("failedAt").asText()))
                .isEqualTo(payment.getCompletedAt());

        assertThat(message.payload().get("retryable").asBoolean()).isFalse();
    }

    private void assertSentState(UUID eventId) {

        OutboxEventEntity sentEvent = outboxRepository.findById(eventId).orElseThrow();

        assertThat(sentEvent.getStatus()).isEqualTo(OutboxStatus.SENT);

        assertThat(sentEvent.getPublishedAt()).isNotNull();

        assertThat(sentEvent.getProcessingOwner()).isNull();

        assertThat(sentEvent.getProcessingStartedAt()).isNull();

        assertThat(sentEvent.getProcessingExpiresAt()).isNull();

        assertThat(sentEvent.getNextAttemptAt()).isNull();

        assertThat(sentEvent.getLastError()).isNull();

        assertThat(sentEvent.getRetryCount()).isZero();
    }

    private ConsumerRecord<String, String> awaitPaymentFailedRecord() {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "payment-failed-publication-verification-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(TOPIC));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

            while (System.nanoTime() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));

                for (ConsumerRecord<String, String> record : records) {

                    return record;
                }
            }
        }

        throw new AssertionError("Expected payment-failed Kafka record");
    }
}
