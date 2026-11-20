package com.cinema.booking.service;

import com.cinema.booking.client.PrintSimulationAccessClient;
import com.cinema.booking.dto.request.PrintSimulationRequest;
import com.cinema.booking.dto.response.PrintSimulationResponse;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.common.exception.exception.NotFoundException;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PrintSimulationService {

    private final PrintSimulationAccessClient access;
    private final BookingRepository bookings;
    private final PrintSimulationTransactionService transaction;

    public PrintSimulationService(
            PrintSimulationAccessClient access,
            BookingRepository bookings,
            PrintSimulationTransactionService transaction) {
        this.access = access;
        this.bookings = bookings;
        this.transaction = transaction;
    }

    public PrintSimulationResponse simulate(
            UUID bookingId, String key, PrintSimulationRequest request) {
        var actor = access.current();
        var booking =
                bookings.findById(bookingId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        var schedule = access.schedule(booking.getShowtimeId());
        actor.requireCinema(schedule.cinemaId());
        return transaction.simulate(bookingId, key, request, actor, schedule);
    }
}
