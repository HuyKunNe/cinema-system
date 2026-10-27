package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.dto.response.ShowtimeSchedulingResponse;
import com.cinema.inventory.dto.response.ShowtimeScreeningVersionResponse;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScheduling;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;

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
    @Mapping(target = "screeningVersion", source = "screeningVersion")
    @Mapping(target = "pricingPolicyId", source = "pricingPolicyVersion.policy.id")
    @Mapping(target = "pricingPolicyVersionId", source = "pricingPolicyVersion.id")
    ShowtimeResponse toResponse(Showtime showtime);

    ShowtimeScreeningVersionResponse toResponse(ShowtimeScreeningVersion screeningVersion);

    ShowtimeSchedulingResponse toResponse(ShowtimeScheduling scheduling);

    List<ShowtimeResponse> toResponses(List<Showtime> showtimes);
}
