package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.entity.Seat;

@Mapper(config = InventoryMapperConfig.class)
public interface SeatMapper {

    @Mapping(
            target = "roomId",
            source = "room.id")
    SeatResponse toResponse(Seat seat);

    List<SeatResponse> toResponses(List<Seat> seats);
}