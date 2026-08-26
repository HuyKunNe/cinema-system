package com.cinema.payment.event.listener;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.serialization.PaymentRequestedMessageReader;
import com.cinema.payment.service.PaymentRequestedConsumerService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.payment.kafka", name = "enabled", havingValue = "true")
public class PaymentRequestedKafkaListener {

    private final PaymentRequestedMessageReader messageReader;

    private final PaymentRequestedConsumerService consumerService;

    public PaymentRequestedKafkaListener(
            PaymentRequestedMessageReader messageReader,
            PaymentRequestedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${cinema.payment.kafka.topics." + "payment-requested:" + "payment-requested}",
            groupId =
                    "${cinema.payment.kafka.consumer-groups."
                            + "payment-requested:"
                            + "payment-request-processing}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
