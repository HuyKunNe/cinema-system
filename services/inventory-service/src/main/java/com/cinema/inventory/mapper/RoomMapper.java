package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.entity.Room;

@Mapper(config = InventoryMapperConfig.class)
public interface RoomMapper {

    @Mapping(
            target = "cinemaId",
            source = "cinema.id")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponses(List<Room> rooms);
}