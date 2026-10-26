package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.dto.response.ShowtimeScreeningVersionResponse;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;

@Mapper(config = InventoryMapperConfig.class)
public interface ShowtimeMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "cinemaId", source = "room.cinema.id")
    @Mapping(target = "cinemaName", source = "room.cinema.name")
    @Mapping(target = "roomLayoutId", source = "roomLayout.id")
    @Mapping(target = "screeningVersion", source = "screeningVersion")
    ShowtimeResponse toResponse(Showtime showtime);

    ShowtimeScreeningVersionResponse toResponse(ShowtimeScreeningVersion screeningVersion);

    List<ShowtimeResponse> toResponses(List<Showtime> showtimes);
}
