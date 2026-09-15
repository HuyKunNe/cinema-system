package com.cinema.inventory.event.listener;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.serialization.SeatReleaseRequestedMessageReader;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;

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
public class SeatReleaseRequestedKafkaListener {

    private final SeatReleaseRequestedMessageReader messageReader;

    private final SeatReleaseRequestedConsumerService consumerService;

    public SeatReleaseRequestedKafkaListener(
            SeatReleaseRequestedMessageReader messageReader,
            SeatReleaseRequestedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics =
                    "${cinema.inventory.kafka.topics."
                            + "seat-release-requested:"
                            + "seat-release-requested}",
            groupId =
                    "${cinema.inventory.kafka.consumer-groups."
                            + "seat-release-requested:"
                            + "inventory-seat-release}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}

