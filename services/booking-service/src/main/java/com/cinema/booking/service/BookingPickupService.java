package com.cinema.booking.service;

import com.cinema.booking.client.PrintSimulationAccessClient;
import com.cinema.booking.client.PrintSimulationAccessClient.Access;
import com.cinema.booking.client.PrintSimulationAccessClient.PickupSchedule;
import com.cinema.booking.dto.response.BookingPickupLookupResponse;
import com.cinema.booking.dto.response.BookingPickupPassResponse;
import com.cinema.booking.enums.BookingPickupStatus;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingPickupPassRepository;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ServiceUnavailableException;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BookingPickupService {

    private final BookingRepository bookings;
    private final BookingPickupPassRepository passes;
    private final PrintSimulationAccessClient client;
    private final BookingPickupTokenCodec tokens;
    private final BookingPickupTransactionService transaction;

    public BookingPickupService(
            BookingRepository bookings,
            BookingPickupPassRepository passes,
            PrintSimulationAccessClient client,
            BookingPickupTokenCodec tokens,
            BookingPickupTransactionService transaction) {
        this.bookings = bookings;
        this.passes = passes;
        this.client = client;
        this.tokens = tokens;
        this.transaction = transaction;
    }

    public BookingPickupPassResponse owner(UUID owner, UUID bookingId) {
        var booking =
                bookings.findByIdAndUserId(bookingId, owner)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        PickupSchedule schedule = null;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            try {
                schedule = schedule(booking.getShowtimeId());
            } catch (ServiceUnavailableException failure) {
                var pending = transaction.owner(owner, bookingId, null);
                if (pending.status() == BookingPickupStatus.WAITING_FOR_TICKETS
                        || pending.status() == BookingPickupStatus.NOT_CONFIRMED) {
                    return pending;
                }
                throw failure;
            }
        }
        return transaction.owner(owner, bookingId, schedule);
    }

    public BookingPickupLookupResponse lookup(String payload) {
        Access access;
        try {
            access = client.current();
        } catch (ForbiddenException failure) {
            throw new ForbiddenException(BookingErrorCode.PICKUP_ACCESS_DENIED);
        } catch (ServiceUnavailableException failure) {
            throw new ServiceUnavailableException(BookingErrorCode.PICKUP_CONTEXT_UNAVAILABLE);
        }
        var pass =
                passes.findByQrToken(tokens.parse(payload))
                        .orElseThrow(() ->
                                new NotFoundException(BookingErrorCode.PICKUP_PASS_NOT_FOUND));
        var booking =
                bookings.findById(pass.getBookingId())
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        return transaction.lookup(
                booking.getId(), pass.getId(), access, schedule(booking.getShowtimeId()));
    }

    private PickupSchedule schedule(UUID showtimeId) {
        try {
            return client.pickupSchedule(showtimeId);
        } catch (ServiceUnavailableException failure) {
            throw new ServiceUnavailableException(BookingErrorCode.PICKUP_CONTEXT_UNAVAILABLE);
        }
    }
}
