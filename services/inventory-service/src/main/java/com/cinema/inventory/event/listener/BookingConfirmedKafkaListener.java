package com.cinema.inventory.event.listener;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.serialization.BookingConfirmedMessageReader;
import com.cinema.inventory.service.BookingConfirmedConsumerService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cinema.inventory.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BookingConfirmedKafkaListener {

    private final BookingConfirmedMessageReader messageReader;

    private final BookingConfirmedConsumerService consumerService;

    public BookingConfirmedKafkaListener(
            BookingConfirmedMessageReader messageReader,
            BookingConfirmedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics =
                    "${cinema.inventory.kafka.topics."
                            + "booking-confirmed:"
                            + "booking-confirmed}",
            groupId =
                    "${cinema.inventory.kafka.consumer-groups."
                            + "booking-confirmed:"
                            + "inventory-booking-confirmed}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
