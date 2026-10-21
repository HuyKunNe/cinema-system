package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingTicketCategoriesResponse;
import com.cinema.inventory.dto.response.CinemaPricingTicketCategoriesResponse.PricingTicketCategoryResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingTicketCategory;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        config = InventoryMapperConfig.class,
        uses = CinemaPricingPolicyMapper.class)
public interface CinemaPricingTicketCategoriesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "ticketCategories", source = "ticketCategories")
    CinemaPricingTicketCategoriesResponse toResponse(
            CinemaPricingPolicy policy,
            List<PricingTicketCategory> ticketCategories);

    PricingTicketCategoryResponse toCategoryResponse(
            PricingTicketCategory category);
}
