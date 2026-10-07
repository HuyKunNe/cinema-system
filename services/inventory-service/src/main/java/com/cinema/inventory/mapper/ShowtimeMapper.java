package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Showtime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class)
public interface ShowtimeMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "cinemaId", source = "room.cinema.id")
    @Mapping(target = "cinemaName", source = "room.cinema.name")
    @Mapping(target = "roomLayoutId", source = "roomLayout.id")
    ShowtimeResponse toResponse(Showtime showtime);

    List<ShowtimeResponse> toResponses(List<Showtime> showtimes);
}
