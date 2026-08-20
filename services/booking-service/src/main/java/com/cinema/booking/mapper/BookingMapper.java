package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.BookingSeatResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.common.mapper.config.MapperConfiguration;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapperConfiguration.class)
public interface BookingMapper {

    @Mapping(target = "id", source = "booking.id")
    @Mapping(target = "userId", source = "booking.userId")
    @Mapping(target = "showtimeId", source = "booking.showtimeId")
    @Mapping(target = "clientRequestId", source = "booking.clientRequestId")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "totalAmount", source = "booking.totalAmount")
    @Mapping(target = "currency", source = "booking.currency")
    @Mapping(target = "expiresAt", source = "booking.expiresAt")
    @Mapping(target = "confirmedAt", source = "booking.confirmedAt")
    @Mapping(target = "cancelledAt", source = "booking.cancelledAt")
    @Mapping(target = "rejectionReason", source = "booking.rejectionReason")
    @Mapping(target = "seats", source = "bookingSeats")
    @Mapping(target = "version", source = "booking.version")
    @Mapping(target = "createdAt", source = "booking.createdAt")
    @Mapping(target = "updatedAt", source = "booking.updatedAt")
    BookingResponse toResponse(Booking booking, List<BookingSeat> bookingSeats);

    BookingSeatResponse toResponse(BookingSeat bookingSeat);
}
