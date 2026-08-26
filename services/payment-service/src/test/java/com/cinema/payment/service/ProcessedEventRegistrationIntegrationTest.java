package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.repository.ProcessedEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

class ProcessedEventRegistrationIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private ProcessedEventRegistrationService service;

    @Autowired private ProcessedEventRepository repository;

    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void sameEventForSameConsumerShouldBeRegisteredOnce() {

        UUID eventId = UuidGenerator.next();

        Boolean first =
                transactionTemplate.execute(
                        status ->
                                service.register(
                                        eventId,
                                        PaymentEventContract.PAYMENT_REQUESTED_CONSUMER,
                                        PaymentEventContract.PAYMENT_REQUESTED,
                                        PaymentEventContract.PAYMENT_REQUESTED_VERSION));

        Boolean duplicate =
                transactionTemplate.execute(
                        status ->
                                service.register(
                                        eventId,
                                        PaymentEventContract.PAYMENT_REQUESTED_CONSUMER,
                                        PaymentEventContract.PAYMENT_REQUESTED,
                                        PaymentEventContract.PAYMENT_REQUESTED_VERSION));

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();

        assertThat(
                        repository.countByEventIdAndConsumerName(
                                eventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isEqualTo(1);
    }

    @Test
    void sameEventForDifferentConsumersShouldBeIndependent() {

        UUID eventId = UuidGenerator.next();

        Boolean first =
                transactionTemplate.execute(
                        status ->
                                service.register(
                                        eventId,
                                        PaymentEventContract.PAYMENT_REQUESTED_CONSUMER,
                                        PaymentEventContract.PAYMENT_REQUESTED,
                                        PaymentEventContract.PAYMENT_REQUESTED_VERSION));

        Boolean second =
                transactionTemplate.execute(
                        status ->
                                service.register(
                                        eventId,
                                        "payment-audit-processing",
                                        PaymentEventContract.PAYMENT_REQUESTED,
                                        PaymentEventContract.PAYMENT_REQUESTED_VERSION));

        assertThat(first).isTrue();
        assertThat(second).isTrue();

        assertThat(
                        repository.countByEventIdAndConsumerName(
                                eventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isEqualTo(1);

        assertThat(repository.countByEventIdAndConsumerName(eventId, "payment-audit-processing"))
                .isEqualTo(1);
    }

    @Test
    void registrationOutsideTransactionShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(),
                                        PaymentEventContract.PAYMENT_REQUESTED_CONSUMER,
                                        PaymentEventContract.PAYMENT_REQUESTED,
                                        PaymentEventContract.PAYMENT_REQUESTED_VERSION))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
