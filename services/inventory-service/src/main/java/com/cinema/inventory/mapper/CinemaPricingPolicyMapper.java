package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;

@Mapper(config = InventoryMapperConfig.class)
public interface CinemaPricingPolicyMapper {

    @Mapping(target = "policyId", source = "policy.id")
    @Mapping(target = "cinemaId", source = "policy.cinema.id")
    CinemaPricingPolicyVersionResponse toResponse(CinemaPricingPolicyVersion revision);

    List<CinemaPricingPolicyVersionResponse> toResponses(
            List<CinemaPricingPolicyVersion> revisions);
}
