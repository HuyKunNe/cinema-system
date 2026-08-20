package com.cinema.booking.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.cinema.booking.entity.BookingSeat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository
        extends JpaRepository<BookingSeat, UUID> {

    List<BookingSeat> findAllByBookingIdOrderBySeatNumberAsc(
            UUID bookingId);

    List<BookingSeat>
            findAllByBookingIdAndSeatNumberInOrderBySeatNumberAsc(
                    UUID bookingId,
                    Collection<String> seatNumbers);

    List<BookingSeat>
            findAllByBookingIdInOrderByBookingIdAscSeatNumberAsc(
                    Collection<UUID> bookingIds);

    boolean existsByBookingIdAndShowtimeIdAndSeatNumber(
            UUID bookingId,
            UUID showtimeId,
            String seatNumber);
}
