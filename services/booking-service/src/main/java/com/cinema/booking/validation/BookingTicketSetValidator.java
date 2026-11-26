package com.cinema.booking.validation;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingTicket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BookingTicketSetValidator {

    private BookingTicketSetValidator() {}

    public static boolean isComplete(
            Booking booking, List<BookingSeat> seats, List<BookingTicket> tickets) {
        return seats.size() == tickets.size() && isConsistentSubset(booking, seats, tickets);
    }

    public static boolean isConsistentSubset(
            Booking booking, List<BookingSeat> seats, List<BookingTicket> tickets) {
        if (seats.isEmpty() || tickets.size() > seats.size()) {
            return false;
        }
        Map<UUID, BookingSeat> seatsById = new HashMap<>();
        Set<String> seatNumbers = new HashSet<>();
        for (var seat : seats) {
            if (seat.getId() == null
                    || !seat.hasCompletedSnapshot()
                    || !booking.getId().equals(seat.getBookingId())
                    || !booking.getShowtimeId().equals(seat.getShowtimeId())
                    || seat.getSeatNumber() == null
                    || !seatNumbers.add(seat.getSeatNumber())
                    || seatsById.putIfAbsent(seat.getId(), seat) != null
                    || !Set.of("COUPLE", "STANDARD", "VIP", "ACCESSIBLE")
                            .contains(seat.getSeatType())) {
                return false;
            }
        }
        Set<UUID> matched = new HashSet<>();
        for (var ticket : tickets) {
            var seat = seatsById.get(ticket.getBookingSeatId());
            if (seat == null
                    || !booking.getId().equals(ticket.getBookingId())
                    || !Objects.equals(ticket.getSeatNumber(), seat.getSeatNumber())
                    || !matched.add(seat.getId())
                    || BookingTicket.capacityFor(seat) != ticket.getCapacity()) {
                return false;
            }
        }
        return true;
    }
}
