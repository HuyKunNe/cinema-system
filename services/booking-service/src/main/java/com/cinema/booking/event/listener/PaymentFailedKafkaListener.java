package com.cinema.booking.event.listener;

import com.cinema.booking.event.serialization.PaymentResultMessageReader;
import com.cinema.booking.service.PaymentFailedConsumerService;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.booking.kafka", name = "enabled", havingValue = "true")
public class PaymentFailedKafkaListener {

    private final PaymentResultMessageReader messageReader;

    private final PaymentFailedConsumerService consumerService;

    public PaymentFailedKafkaListener(
            PaymentResultMessageReader messageReader,
            PaymentFailedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${cinema.booking.kafka.topics." + "payment-failed:" + "payment-failed}",
            groupId =
                    "${cinema.booking.kafka.consumer-groups."
                            + "payment-failed:"
                            + "booking-payment-failed}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
