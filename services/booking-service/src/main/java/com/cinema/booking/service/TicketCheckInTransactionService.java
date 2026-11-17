package com.cinema.booking.service;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TicketCheckInTransactionService {
    private final BookingRepository bookings;
    private final BookingTicketRepository tickets;
    private final Clock clock;
    public TicketCheckInTransactionService(BookingRepository bookings, BookingTicketRepository tickets,
            @Qualifier("systemClock") Clock clock) {this.bookings=bookings;this.tickets=tickets;this.clock=clock;}
    @Transactional
    public Result consume(BookingTicketService.LookupResponse expected, OffsetDateTime startsAt, UUID actor) {
        var booking=bookings.findByIdForUpdate(expected.bookingId())
                .orElseThrow(()->new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        var ticket=tickets.findByIdForUpdate(expected.ticketId())
                .orElseThrow(()->new NotFoundException(BookingErrorCode.TICKET_NOT_FOUND));
        if(!ticket.getBookingId().equals(booking.getId()) || !booking.getShowtimeId().equals(expected.showtimeId())
                || booking.getStatus()!=BookingStatus.CONFIRMED) {
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        }
        if("USED".equals(ticket.getStatus())) return new Result(ticket.getId(), booking.getId(),
                ticket.getSeatNumber(),ticket.getCapacity(),"ALREADY_USED",ticket.getCheckedInAt());
        if(startsAt==null) throw new ConflictException(BookingErrorCode.TICKET_SCHEDULE_UNAVAILABLE);
        ticket.checkIn(OffsetDateTime.now(clock),actor,startsAt);
        return new Result(ticket.getId(),booking.getId(),ticket.getSeatNumber(),ticket.getCapacity(),
                "CHECKED_IN",ticket.getCheckedInAt());
    }
    public record Result(UUID ticketId,UUID bookingId,String seatNumber,int capacity,String result,OffsetDateTime checkedInAt) {}
}
