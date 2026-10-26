package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class)
public interface CinemaPricingPolicyMapper {

    @Mapping(target = "policyId", source = "policy.id")
    @Mapping(
            target = "cinemaIds",
            expression = "java(revision.getCinemaIds().stream().sorted().toList())")
    CinemaPricingPolicyVersionResponse toResponse(CinemaPricingPolicyVersion revision);

    List<CinemaPricingPolicyVersionResponse> toResponses(
            List<CinemaPricingPolicyVersion> revisions);
}
