package com.cinema.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.service.PaymentProviderOperationWorker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@SpringBootTest(
        properties = {
            "spring.main.web-application-type=none",
            "cinema.payment.kafka.enabled=false",
            "cinema.payment.provider=MOCK"
        })
@ActiveProfiles("test")
class PaymentProviderOperationTransactionIntegrationTest {

    @Autowired private PaymentProviderOperationExecutor executor;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private PaymentProviderOperationWorker operationWorker;

    @Test
    void executionOutsideTransactionShouldBeAllowed() {

        ClaimedProviderChargeOperation operation = operation();

        ProviderChargeResult result = executor.execute(operation);

        assertThat(result.outcome()).isEqualTo(ProviderOutcome.SUCCEEDED);

        assertThat(result.providerReference()).isEqualTo("mock-" + operation.idempotencyKey());
    }

    @Test
    void executionInsideTransactionShouldBeRejectedBeforeProviderCall() {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        ClaimedProviderChargeOperation operation = operation();

        assertThatThrownBy(() -> transactionTemplate.execute(status -> executor.execute(operation)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void workerInsideExistingTransactionShouldBeRejected() {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(
                        () ->
                                transactionTemplate.executeWithoutResult(
                                        status -> operationWorker.processNextBatch()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static ClaimedProviderChargeOperation operation() {

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        new BigDecimal("125000.00"),
                        "VND",
                        OffsetDateTime.parse("2026-09-08T12:00:00Z"));

        return new ClaimedProviderChargeOperation(
                UuidGenerator.next(),
                "payment-provider-operation:worker-1",
                "MOCK",
                "charge:01991d2b-bd4a-7000-8000-000000000001",
                command);
    }
}
