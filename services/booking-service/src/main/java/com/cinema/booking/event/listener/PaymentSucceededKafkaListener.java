package com.cinema.booking.event.listener;

import com.cinema.booking.event.serialization.PaymentResultMessageReader;
import com.cinema.booking.service.PaymentSucceededConsumerService;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.booking.kafka", name = "enabled", havingValue = "true")
public class PaymentSucceededKafkaListener {

    private final PaymentResultMessageReader messageReader;

    private final PaymentSucceededConsumerService consumerService;

    public PaymentSucceededKafkaListener(
            PaymentResultMessageReader messageReader,
            PaymentSucceededConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${cinema.booking.kafka.topics." + "payment-succeeded:" + "payment-succeeded}",
            groupId =
                    "${cinema.booking.kafka.consumer-groups."
                            + "payment-succeeded:"
                            + "booking-payment-succeeded}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
