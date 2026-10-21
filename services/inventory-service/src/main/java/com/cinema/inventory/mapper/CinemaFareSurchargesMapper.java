package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaFareSurchargesResponse;
import com.cinema.inventory.dto.response.CinemaFareSurchargesResponse.FareSurchargeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareSurchargeRule;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        config = InventoryMapperConfig.class,
        uses = CinemaPricingPolicyMapper.class)
public interface CinemaFareSurchargesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "mode", source = "policy.fareSurchargeMode")
    @Mapping(target = "surcharges", source = "surcharges")
    CinemaFareSurchargesResponse toResponse(
            CinemaPricingPolicy policy,
            List<FareSurchargeRule> surcharges);

    @Mapping(
            target = "amountVnd",
            expression = "java(Long.toString(rule.getAmountVnd()))")
    FareSurchargeResponse toRuleResponse(FareSurchargeRule rule);
}
