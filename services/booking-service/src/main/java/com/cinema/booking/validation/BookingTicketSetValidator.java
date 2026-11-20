package com.cinema.booking.validation;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingTicket;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BookingTicketSetValidator {

    private BookingTicketSetValidator() {}

    public static boolean isComplete(
            Booking booking, List<BookingSeat> seats, List<BookingTicket> tickets) {
        if (seats.isEmpty() || seats.size() != tickets.size()) {
            return false;
        }
        Set<UUID> matched = new HashSet<>();
        for (var ticket : tickets) {
            var seat =
                    seats.stream()
                            .filter(s -> s.getId().equals(ticket.getBookingSeatId()))
                            .findFirst()
                            .orElse(null);
            if (seat == null
                    || !seat.hasCompletedSnapshot()
                    || !booking.getId().equals(seat.getBookingId())
                    || !booking.getShowtimeId().equals(seat.getShowtimeId())
                    || !booking.getId().equals(ticket.getBookingId())
                    || !Objects.equals(ticket.getSeatNumber(), seat.getSeatNumber())
                    || !matched.add(seat.getId())
                    || !Set.of("COUPLE", "STANDARD", "VIP", "ACCESSIBLE")
                            .contains(seat.getSeatType())
                    || BookingTicket.capacityFor(seat) != ticket.getCapacity()) {
                return false;
            }
        }
        return true;
    }
}
