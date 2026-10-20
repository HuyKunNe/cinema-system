package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaSurchargesResponse;
import com.cinema.inventory.dto.response.CinemaSurchargesResponse.SurchargeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketSurchargeRule;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        config = InventoryMapperConfig.class,
        uses = CinemaPricingPolicyMapper.class)
public interface CinemaSurchargesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "surcharges", source = "surcharges")
    CinemaSurchargesResponse toResponse(
            CinemaPricingPolicy policy,
            List<TicketSurchargeRule> surcharges);

    SurchargeResponse toSurchargeResponse(TicketSurchargeRule rule);
}
