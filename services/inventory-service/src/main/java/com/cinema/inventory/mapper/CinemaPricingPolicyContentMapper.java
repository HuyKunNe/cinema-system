package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse;
import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse.RuleResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Mapper(config = InventoryMapperConfig.class, uses = CinemaPricingPolicyMapper.class)
public interface CinemaPricingPolicyContentMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "rules", source = "rules")
    CinemaPricingPolicyContentResponse toResponse(
            CinemaPricingPolicy policy, List<TicketPriceRule> rules);

    @Mapping(target = "daysOfWeek", source = "daysOfWeek", qualifiedByName = "orderedDays")
    RuleResponse toRuleResponse(TicketPriceRule rule);

    @Named("orderedDays")
    default List<DayOfWeek> orderedDays(Set<DayOfWeek> days) {
        return days.stream().sorted().toList();
    }
}
