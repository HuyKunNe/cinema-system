package com.cinema.inventory.event.listener;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.serialization.BookingCancelledMessageReader;
import com.cinema.inventory.service.BookingCancelledConsumerService;

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
public class BookingCancelledKafkaListener {

    private final BookingCancelledMessageReader messageReader;

    private final BookingCancelledConsumerService consumerService;

    public BookingCancelledKafkaListener(
            BookingCancelledMessageReader messageReader,
            BookingCancelledConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics =
                    "${cinema.inventory.kafka.topics."
                            + "booking-cancelled:"
                            + "booking-cancelled}",
            groupId =
                    "${cinema.inventory.kafka.consumer-groups."
                            + "booking-cancelled:"
                            + "inventory-booking-cancelled}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
