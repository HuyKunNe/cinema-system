package com.cinema.payment.provider.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.test.annotation.IntegrationTest;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.provider.webhook.model.ProviderWebhookRequest;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@IntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PaymentProviderWebhookHttpIntegrationTest.WebhookTestConfiguration.class)
@TestPropertySource(
        properties = {
            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.payment.webhook.maximum-body-size=4KB"
        })
class PaymentProviderWebhookHttpIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-10T10:00:00Z");

    private static final BigDecimal AMOUNT = new BigDecimal("125000.00");

    private static final String PROVIDER = "TESTPAY";

    private static final String PROVIDER_REFERENCE = "test-provider-reference-001";

    private static final String VALID_SIGNATURE = "valid-test-signature";

    private static final String ACKNOWLEDGEMENT_JSON = "{\"accepted\":true}";

    private static final String PROCESSING_OWNER = "webhook-http-integration-worker";

    @Autowired private MockMvc mockMvc;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private PaymentProviderWebhookEventRepository webhookEventRepository;

    @BeforeEach
    void cleanDatabase() {
        webhookEventRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
    }

    @Test
    void successfulWebhookShouldApplyPaymentAndReturnAcknowledgement() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        byte[] requestBody = webhookBody("http-success-event", "SUCCEEDED", "125000.00");

        performWebhook(PROVIDER, VALID_SIGNATURE, requestBody)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ACKNOWLEDGEMENT_JSON));

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, "http-success-event"))
                .isEqualTo(1);

        Payment payment = paymentRepository.findById(aggregate.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(aggregate.transactionId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(payment.getProviderReference()).isEqualTo(PROVIDER_REFERENCE);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(transaction.getProviderEventId()).isEqualTo("http-success-event");
    }

    @Test
    void duplicateWebhookShouldReturnSameAcknowledgementWithoutSecondEffect() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        byte[] requestBody = webhookBody("http-duplicate-event", "SUCCEEDED", "125000.00");

        performWebhook(PROVIDER, VALID_SIGNATURE, requestBody)
                .andExpect(status().isOk())
                .andExpect(content().json(ACKNOWLEDGEMENT_JSON));

        performWebhook(PROVIDER, VALID_SIGNATURE, requestBody)
                .andExpect(status().isOk())
                .andExpect(content().json(ACKNOWLEDGEMENT_JSON));

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, "http-duplicate-event"))
                .isEqualTo(1);

        assertThat(webhookEventRepository.count()).isEqualTo(1);

        Payment payment = paymentRepository.findById(aggregate.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(aggregate.transactionId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);
    }

    @Test
    void invalidSignatureShouldReturnUnauthorizedWithoutPersistence() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        byte[] requestBody = webhookBody("invalid-signature-event", "SUCCEEDED", "125000.00");

        performWebhook(PROVIDER, "invalid-signature", requestBody)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.error.code")
                                .value("PAYMENT_PROVIDER_WEBHOOK_AUTHENTICATION_FAILED"));

        assertThat(webhookEventRepository.count()).isZero();

        assertPendingAggregate(aggregate);
    }

    @Test
    void unsupportedProviderShouldReturnBadRequestWithoutPersistence() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        byte[] requestBody = webhookBody("unsupported-provider-event", "SUCCEEDED", "125000.00");

        performWebhook("UNKNOWN", VALID_SIGNATURE, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.error.code").value("PAYMENT_PROVIDER_WEBHOOK_NOT_SUPPORTED"));

        assertThat(webhookEventRepository.count()).isZero();

        assertPendingAggregate(aggregate);
    }

    @Test
    void oversizedBodyShouldBeRejectedBeforeVerification() throws Exception {

        byte[] oversizedBody = new byte[4_097];

        performWebhook(PROVIDER, VALID_SIGNATURE, oversizedBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.error.code").value("PAYMENT_PROVIDER_WEBHOOK_BODY_TOO_LARGE"));

        assertThat(webhookEventRepository.count()).isZero();
    }

    @Test
    void mismatchedAmountShouldReturnSanitizedFailureWithoutMarker() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        byte[] requestBody = webhookBody("amount-mismatch-event", "SUCCEEDED", "130000.00");

        performWebhook(PROVIDER, VALID_SIGNATURE, requestBody)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PAYMENT_TRANSACTION_DATA_MISMATCH"))
                .andExpect(
                        jsonPath("$.error.message")
                                .value("Payment transaction data does not match its payment"));

        assertThat(webhookEventRepository.count()).isZero();

        assertPendingAggregate(aggregate);
    }

    private org.springframework.test.web.servlet.ResultActions performWebhook(
            String provider, String signature, byte[] requestBody) throws Exception {

        return mockMvc.perform(
                post("/api/v1/payments/webhooks/{provider}", provider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TestPaymentProviderWebhookVerifier.SIGNATURE_HEADER, signature)
                        .content(requestBody));
    }

    private TestAggregate persistPendingAggregate() {
        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        AMOUNT,
                        "VND",
                        PROVIDER,
                        NOW.plusMinutes(10),
                        NOW.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.recordPendingProvider(PROVIDER_REFERENCE, NOW.minusSeconds(10));

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        PROVIDER,
                        PaymentTransactionType.CHARGE,
                        1,
                        AMOUNT,
                        "VND",
                        "charge:" + payment.getId(),
                        NOW.minusMinutes(1));

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(20), NOW.plusMinutes(1));

        transaction.markPendingProvider(PROCESSING_OWNER, PROVIDER_REFERENCE, null, null);

        transactionRepository.saveAndFlush(transaction);

        return new TestAggregate(payment.getId(), transaction.getId());
    }

    private void assertPendingAggregate(TestAggregate aggregate) {
        Payment payment = paymentRepository.findById(aggregate.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(aggregate.transactionId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING_PROVIDER);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(transaction.getProviderEventId()).isNull();
    }

    private static byte[] webhookBody(String providerEventId, String outcome, String amount) {

        String json =
                """
                {
                  "providerEventId": "%s",
                  "providerReference": "%s",
                  "outcome": "%s",
                  "amount": "%s",
                  "currency": "VND",
                  "occurredAt": "2026-09-10T09:59:59Z"
                }
                """
                        .formatted(providerEventId, PROVIDER_REFERENCE, outcome, amount);

        return json.getBytes(StandardCharsets.UTF_8);
    }

    private record TestAggregate(java.util.UUID paymentId, java.util.UUID transactionId) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class WebhookTestConfiguration {

        @Bean
        @Primary
        Clock paymentWebhookHttpIntegrationClock() {
            return Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        PaymentProviderWebhookVerifier testPaymentProviderWebhookVerifier(
                ObjectMapper objectMapper) {

            return new TestPaymentProviderWebhookVerifier(objectMapper);
        }
    }

    static final class TestPaymentProviderWebhookVerifier
            implements PaymentProviderWebhookVerifier {

        static final String SIGNATURE_HEADER = "X-Test-Signature";

        private final ObjectMapper objectMapper;

        TestPaymentProviderWebhookVerifier(ObjectMapper objectMapper) {

            this.objectMapper = objectMapper;
        }

        @Override
        public String providerCode() {
            return PROVIDER;
        }

        @Override
        public VerifiedProviderWebhook verifyAndParse(ProviderWebhookRequest request) {

            String signature = request.firstHeader(SIGNATURE_HEADER).orElse(null);

            if (!VALID_SIGNATURE.equals(signature)) {
                throw new UnauthorizedException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_AUTHENTICATION_FAILED);
            }

            JsonNode root = readBody(request.rawBody());

            ProviderOutcome outcome = readOutcome(text(root, "outcome"));

            BigDecimal amount = readAmount(text(root, "amount"));

            OffsetDateTime occurredAt = readOccurredAt(text(root, "occurredAt"));

            return new VerifiedProviderWebhook(
                    request.provider(),
                    text(root, "providerEventId"),
                    text(root, "providerReference"),
                    outcome,
                    amount,
                    text(root, "currency"),
                    occurredAt,
                    text(root, "failureCode"),
                    text(root, "failureMessage"));
        }

        @Override
        public ProviderWebhookAcknowledgement acknowledgement(
                PaymentProviderWebhookApplicationResult result) {

            return new ProviderWebhookAcknowledgement(
                    HttpStatus.OK,
                    MediaType.APPLICATION_JSON,
                    ACKNOWLEDGEMENT_JSON.getBytes(StandardCharsets.UTF_8));
        }

        private JsonNode readBody(byte[] rawBody) {
            try {
                return objectMapper.readTree(rawBody);

            } catch (IOException exception) {
                throw new ValidationException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID, exception);
            }
        }

        private static ProviderOutcome readOutcome(String value) {

            if (value == null) {
                return null;
            }

            try {
                return ProviderOutcome.valueOf(value);

            } catch (IllegalArgumentException exception) {
                throw new ValidationException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID, exception);
            }
        }

        private static BigDecimal readAmount(String value) {
            if (value == null) {
                return null;
            }

            try {
                return new BigDecimal(value);

            } catch (NumberFormatException exception) {
                throw new ValidationException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID, exception);
            }
        }

        private static OffsetDateTime readOccurredAt(String value) {

            if (value == null) {
                return null;
            }

            try {
                return OffsetDateTime.parse(value);

            } catch (DateTimeException exception) {
                throw new ValidationException(
                        PaymentErrorCode.PROVIDER_WEBHOOK_RESULT_INVALID, exception);
            }
        }

        private static String text(JsonNode root, String fieldName) {

            JsonNode value = root.get(fieldName);

            if (value == null || value.isNull()) {
                return null;
            }

            return value.asText();
        }
    }
}
