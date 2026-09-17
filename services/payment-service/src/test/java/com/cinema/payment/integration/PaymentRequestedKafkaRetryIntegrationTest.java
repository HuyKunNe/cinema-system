package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ProcessedEventRepository;
import com.cinema.payment.service.PaymentRequestedConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class PaymentRequestedKafkaRetryIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = "payment-requested";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "payment-request-retry-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final OffsetDateTime REQUESTED_AT =
            OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

    private static final BigDecimal AMOUNT = new BigDecimal("210000.00");

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

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

    @MockitoBean private PaymentRequestedConsumerService consumerService;

    @Test
    void retryableFailureShouldRetryTwiceThenPublishToDeadLetterTopic() throws Exception {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = paymentRequestedMessage(bookingId);

        String serializedMessage = objectMapper.writeValueAsString(message);

        /*
         * RuntimeException is retryable because only ValidationException
         * is explicitly classified as non-retryable.
         */
        doThrow(new IllegalStateException("forced retryable failure"))
                .when(consumerService)
                .handle(anyString(), any(OutboxEventMessage.class));

        kafkaTemplate
                .send(TOPIC, bookingId.toString(), serializedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord = awaitDeadLetterRecord();

        /*
         * maximumRetries = 2 means:
         *
         * initial delivery
         * + retry #1
         * + retry #2
         *
         * = exactly 3 service invocations.
         */
        verify(consumerService, timeout(10_000).times(3))
                .handle(anyString(), any(OutboxEventMessage.class));

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(bookingId.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(serializedMessage);

        /*
         * DLT must not expose exception implementation details.
         */
        Headers headers = deadLetterRecord.headers();

        assertThat(headers.lastHeader("kafka_dlt-exception")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-cause-fqcn")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();

        /*
         * The mocked application-service boundary fails before
         * business persistence can commit.
         */
        assertThat(paymentRepository.count()).isZero();

        assertThat(transactionRepository.count()).isZero();

        assertThat(processedEventRepository.count()).isZero();
    }

    private OutboxEventMessage paymentRequestedMessage(UUID bookingId) {

        UUID userId = UuidGenerator.next();

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID causationId = UuidGenerator.next();

        OffsetDateTime holdExpiresAt = REQUESTED_AT.plusMinutes(30);

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        bookingId, userId, AMOUNT, "VND", 1, holdExpiresAt, REQUESTED_AT);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                "BOOKING",
                "payment-requested",
                "1",
                REQUESTED_AT,
                "booking-service",
                correlationId,
                causationId,
                objectMapper.valueToTree(payload));
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord() throws Exception {

        Map<String, Object> consumerProperties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "payment-request-retry-dlt-verification-" + UuidGenerator.next(),
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

                    return record;
                }
            }
        }

        throw new AssertionError("Expected record on dead-letter topic " + DEAD_LETTER_TOPIC);
    }
}
