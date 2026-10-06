package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.RoomLayoutResponse;
import com.cinema.inventory.entity.RoomLayout;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class)
public interface RoomLayoutMapper {

    @Mapping(target = "roomId", source = "room.id")
    RoomLayoutResponse toResponse(RoomLayout layout);

    List<RoomLayoutResponse> toResponses(List<RoomLayout> layouts);
}
