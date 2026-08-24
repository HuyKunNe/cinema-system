package com.cinema.inventory.event.listener;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.serialization.SeatReservationRequestedMessageReader;
import com.cinema.inventory.service.SeatReservationRequestedConsumerService;

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
public class SeatReservationRequestedKafkaListener {

    private final SeatReservationRequestedMessageReader messageReader;

    private final SeatReservationRequestedConsumerService consumerService;

    public SeatReservationRequestedKafkaListener(
            SeatReservationRequestedMessageReader messageReader,
            SeatReservationRequestedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics =
                    "${cinema.inventory.kafka.topics."
                            + "seat-reservation-requested:"
                            + "seat-reservation-requested}",
            groupId = "${cinema.inventory.kafka.consumer-group:" + "inventory-seat-reservation}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
