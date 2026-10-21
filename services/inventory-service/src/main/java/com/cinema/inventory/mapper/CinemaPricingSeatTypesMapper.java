package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingSeatTypesResponse;
import com.cinema.inventory.dto.response.CinemaPricingSeatTypesResponse.PricingSeatTypeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSeatType;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        config = InventoryMapperConfig.class,
        uses = CinemaPricingPolicyMapper.class)
public interface CinemaPricingSeatTypesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "seatTypes", source = "seatTypes")
    CinemaPricingSeatTypesResponse toResponse(
            CinemaPricingPolicy policy,
            List<PricingSeatType> seatTypes);

    PricingSeatTypeResponse toSeatTypeResponse(PricingSeatType seatType);
}
