package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingSaleScopesResponse;
import com.cinema.inventory.dto.response.CinemaPricingSaleScopesResponse.SaleScopeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSaleScope;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class, uses = CinemaPricingPolicyMapper.class)
public interface CinemaPricingSaleScopesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "saleScopes", source = "saleScopes")
    CinemaPricingSaleScopesResponse toResponse(
            CinemaPricingPolicy policy, List<PricingSaleScope> saleScopes);

    SaleScopeResponse toScopeResponse(PricingSaleScope scope);
}
