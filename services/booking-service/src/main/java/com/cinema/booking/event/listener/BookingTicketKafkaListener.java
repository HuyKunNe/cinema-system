package com.cinema.booking.event.listener;

import com.cinema.booking.service.BookingTicketService;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cinema.booking.kafka", name = "enabled", havingValue = "true")
public class BookingTicketKafkaListener {
    private final ObjectMapper mapper;
    private final BookingTicketService service;

    public BookingTicketKafkaListener(ObjectMapper mapper, BookingTicketService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @KafkaListener(
            topics = "${cinema.booking.kafka.topics.booking-confirmed:booking-confirmed}",
            groupId =
                    "${cinema.booking.kafka.consumer-groups.ticket-issuance:booking-ticket-issuance}")
    public void consume(@Header(KafkaHeaders.RECEIVED_KEY) String key, String serialized)
            throws JsonProcessingException {
        service.issue(key, mapper.readValue(serialized, OutboxEventMessage.class));
    }
}
