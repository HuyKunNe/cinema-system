package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.entity.ShowSeat;

@Mapper(config = InventoryMapperConfig.class)
public interface ShowSeatMapper {

    @Mapping(
            target = "showtimeId",
            source = "showtime.id")
    @Mapping(
            target = "seatId",
            source = "seat.id")
    ShowSeatResponse toResponse(ShowSeat showSeat);

    List<ShowSeatResponse> toResponses(
            List<ShowSeat> showSeats);
}