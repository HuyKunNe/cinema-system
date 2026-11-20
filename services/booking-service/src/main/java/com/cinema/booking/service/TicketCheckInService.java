package com.cinema.booking.service;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketCheckInService {
    private final BookingTicketService tickets;
    private final TicketCheckInTransactionService transaction;
    private final RestClient inventory;

    public TicketCheckInService(
            BookingTicketService tickets,
            TicketCheckInTransactionService transaction,
            @Value("${cinema.booking.inventory-base-url:http://localhost:8083}") String baseUrl) {
        this.tickets = tickets;
        this.transaction = transaction;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(1))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        inventory = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public TicketCheckInTransactionService.Result checkIn(String payload, UUID actor) {
        var ticket = tickets.lookup(payload);
        if ("USED".equals(ticket.ticketStatus())) return transaction.consume(ticket, null, actor);
        Schedule schedule;
        try {
            schedule =
                    inventory
                            .get()
                            .uri("/api/v1/showtimes/{id}", ticket.showtimeId())
                            .retrieve()
                            .body(Schedule.class);
        } catch (RestClientException failure) {
            throw new InternalServerException(
                    BookingErrorCode.TICKET_SCHEDULE_UNAVAILABLE, failure);
        }
        if (schedule == null
                || !ticket.showtimeId().equals(schedule.id())
                || schedule.startsAt() == null
                || schedule.endsAt() == null
                || !schedule.endsAt().isAfter(schedule.startsAt())) {
            throw new InternalServerException(BookingErrorCode.TICKET_SCHEDULE_UNAVAILABLE);
        }
        if (!Set.of("SCHEDULED", "OPEN_FOR_BOOKING", "CLOSED")
                .contains(schedule.status() == null ? "" : schedule.status())) {
            throw new ConflictException(BookingErrorCode.TICKET_CHECK_IN_NOT_ALLOWED);
        }
        return transaction.consume(ticket, schedule.startsAt(), actor);
    }

    private record Schedule(
            UUID id, OffsetDateTime startsAt, OffsetDateTime endsAt, String status) {}
}
