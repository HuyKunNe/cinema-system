package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class)
public interface CinemaPricingPolicyMapper {

    @Mapping(target = "cinemaId", source = "cinema.id")
    CinemaPricingPolicyResponse toResponse(CinemaPricingPolicy policy);

    List<CinemaPricingPolicyResponse> toResponses(
            List<CinemaPricingPolicy> policies);
}
