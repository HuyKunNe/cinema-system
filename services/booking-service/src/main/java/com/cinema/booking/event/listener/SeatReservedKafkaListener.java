package com.cinema.booking.event.listener;

import com.cinema.booking.event.serialization.SeatReservedMessageReader;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.booking.kafka", name = "enabled", havingValue = "true")
public class SeatReservedKafkaListener {

    private final SeatReservedMessageReader messageReader;

    private final SeatReservedConsumerService consumerService;

    public SeatReservedKafkaListener(
            SeatReservedMessageReader messageReader, SeatReservedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${cinema.booking.kafka.topics." + "seat-reserved:" + "seat-reserved}",
            groupId =
                    "${cinema.booking.kafka.consumer-groups."
                            + "seat-reserved:"
                            + "booking-seat-reserved}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
