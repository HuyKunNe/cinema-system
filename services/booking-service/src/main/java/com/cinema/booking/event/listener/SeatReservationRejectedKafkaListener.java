package com.cinema.booking.event.listener;

import com.cinema.booking.event.serialization.SeatReservationRejectedMessageReader;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.booking.kafka", name = "enabled", havingValue = "true")
public class SeatReservationRejectedKafkaListener {

    private final SeatReservationRejectedMessageReader messageReader;

    private final SeatReservationRejectedConsumerService consumerService;

    public SeatReservationRejectedKafkaListener(
            SeatReservationRejectedMessageReader messageReader,
            SeatReservationRejectedConsumerService consumerService) {

        this.messageReader = messageReader;
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics =
                    "${cinema.booking.kafka.topics."
                            + "seat-reservation-rejected:"
                            + "seat-reservation-rejected}",
            groupId =
                    "${cinema.booking.kafka.consumer-groups."
                            + "seat-reservation-rejected:"
                            + "booking-seat-rejected}")
    public void consume(
            @Header(KafkaHeaders.RECEIVED_KEY) String partitionKey, String serializedMessage) {

        OutboxEventMessage message = messageReader.read(serializedMessage);

        consumerService.handle(partitionKey, message);
    }
}
