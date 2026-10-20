package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse;
import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse.RuleResponse;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.entity.TicketSurchargeRule;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Mapper(
        config = InventoryMapperConfig.class,
        uses = {
            CinemaPricingPolicyMapper.class,
            CinemaHolidayDatesMapper.class,
            CinemaSurchargesMapper.class
        })
public interface CinemaPricingPolicyContentMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "rules", source = "rules")
    @Mapping(target = "holidayDates", source = "holidayDates")
    @Mapping(target = "surcharges", source = "surcharges")
    CinemaPricingPolicyContentResponse toResponse(
            CinemaPricingPolicy policy,
            List<TicketPriceRule> rules,
            List<CinemaHolidayDate> holidayDates,
            List<TicketSurchargeRule> surcharges);

    @Mapping(target = "daysOfWeek", source = "daysOfWeek", qualifiedByName = "orderedDays")
    RuleResponse toRuleResponse(TicketPriceRule rule);

    @Named("orderedDays")
    default List<DayOfWeek> orderedDays(Set<DayOfWeek> days) {
        return days.stream().sorted().toList();
    }
}
