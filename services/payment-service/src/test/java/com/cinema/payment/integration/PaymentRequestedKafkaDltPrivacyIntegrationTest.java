package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class PaymentRequestedKafkaDltPrivacyIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String TOPIC = "payment-requested";

    private static final String DEAD_LETTER_TOPIC = TOPIC + ".dlt";

    private static final String CONSUMER_GROUP = "payment-request-dlt-privacy-integration";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private static final String SAFE_HEADER = "x-cinema-test-correlation";

    private static final String SAFE_HEADER_VALUE = "safe-correlation-value";

    private static final String SENSITIVE_MARKER = "do-not-leak-payment-exception-detail";

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

    @Test
    void deadLetterPublicationShouldRemoveExceptionDetailsAndPreserveSafeMetadata()
            throws Exception {

        UUID bookingId = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "payment-requested",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        ProducerRecord<String, String> sourceRecord =
                new ProducerRecord<>(TOPIC, bookingId.toString(), malformedMessage);

        /*
         * Simulate a record that already contains exception metadata from
         * an earlier dead-letter or retry stage.
         */
        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_EXCEPTION_FQCN,
                "java.lang.IllegalStateException:" + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN,
                "com.example.PaymentProviderException:" + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                "provider response contained " + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_EXCEPTION_STACKTRACE,
                "stacktrace:" + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_KEY_EXCEPTION_FQCN,
                "java.lang.IllegalArgumentException:" + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_KEY_EXCEPTION_MESSAGE,
                "key failure:" + SENSITIVE_MARKER);

        addHeader(
                sourceRecord.headers(),
                KafkaHeaders.DLT_KEY_EXCEPTION_STACKTRACE,
                "key stacktrace:" + SENSITIVE_MARKER);

        addHeader(sourceRecord.headers(), SAFE_HEADER, SAFE_HEADER_VALUE);

        kafkaTemplate.send(sourceRecord).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord = awaitDeadLetterRecord();

        assertThat(deadLetterRecord.topic()).isEqualTo(DEAD_LETTER_TOPIC);

        assertThat(deadLetterRecord.key()).isEqualTo(bookingId.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        Headers headers = deadLetterRecord.headers();

        assertExceptionHeadersRemoved(headers);

        assertSafeApplicationHeaderPreserved(headers);

        assertOriginalRoutingMetadataPreserved(headers);

        assertNoHeaderContainsSensitiveMarker(headers);
    }

    private static void assertExceptionHeadersRemoved(Headers headers) {

        assertThat(headers.lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_KEY_EXCEPTION_FQCN)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_KEY_EXCEPTION_MESSAGE)).isNull();

        assertThat(headers.lastHeader(KafkaHeaders.DLT_KEY_EXCEPTION_STACKTRACE)).isNull();
    }

    private static void assertSafeApplicationHeaderPreserved(Headers headers) {

        Header safeHeader = headers.lastHeader(SAFE_HEADER);

        assertThat(safeHeader).isNotNull();

        assertThat(new String(safeHeader.value(), UTF_8)).isEqualTo(SAFE_HEADER_VALUE);
    }

    private static void assertOriginalRoutingMetadataPreserved(Headers headers) {

        Header originalTopic = headers.lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);

        Header originalPartition = headers.lastHeader(KafkaHeaders.DLT_ORIGINAL_PARTITION);

        Header originalOffset = headers.lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET);

        Header originalConsumerGroup = headers.lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP);

        assertThat(originalTopic).isNotNull();

        assertThat(new String(originalTopic.value(), UTF_8)).isEqualTo(TOPIC);

        assertThat(originalPartition).isNotNull();

        assertThat(ByteBuffer.wrap(originalPartition.value()).getInt()).isGreaterThanOrEqualTo(0);

        assertThat(originalOffset).isNotNull();

        assertThat(ByteBuffer.wrap(originalOffset.value()).getLong()).isGreaterThanOrEqualTo(0L);

        assertThat(originalConsumerGroup).isNotNull();

        assertThat(new String(originalConsumerGroup.value(), UTF_8)).isEqualTo(CONSUMER_GROUP);
    }

    private static void assertNoHeaderContainsSensitiveMarker(Headers headers) {

        for (Header header : headers) {

            byte[] value = header.value();

            if (value == null) {
                continue;
            }

            if (header.key().equals(SAFE_HEADER)
                    || header.key().startsWith("kafka_dlt-exception")
                    || header.key().startsWith("kafka_dlt-key-exception")) {

                assertThat(new String(value, UTF_8)).doesNotContain(SENSITIVE_MARKER);
            }
        }
    }

    private static void addHeader(Headers headers, String name, String value) {

        headers.add(name, value.getBytes(UTF_8));
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord() {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "payment-dlt-privacy-verification-" + UuidGenerator.next(),
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

        throw new AssertionError("Expected record on dead-letter topic " + DEAD_LETTER_TOPIC);
    }
}
