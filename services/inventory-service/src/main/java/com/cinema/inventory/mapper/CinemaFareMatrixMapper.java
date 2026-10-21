package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaFareMatrixResponse;
import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixDayGroupResponse;
import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixFareRuleResponse;
import com.cinema.inventory.dto.response.CinemaFareMatrixResponse.MatrixTimeBandResponse;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.PricingDayGroup;
import com.cinema.inventory.entity.PricingTimeBand;

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
            CinemaHolidayDatesMapper.class
        })
public interface CinemaFareMatrixMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "dayGroups", source = "dayGroups")
    @Mapping(target = "timeBands", source = "timeBands")
    @Mapping(target = "fareRules", source = "fareRules")
    @Mapping(target = "holidayDates", source = "holidayDates")
    CinemaFareMatrixResponse toResponse(
            CinemaPricingPolicy policy,
            List<PricingDayGroup> dayGroups,
            List<PricingTimeBand> timeBands,
            List<FareRule> fareRules,
            List<CinemaHolidayDate> holidayDates);

    @Mapping(
            target = "daysOfWeek",
            source = "daysOfWeek",
            qualifiedByName = "orderedMatrixDays")
    MatrixDayGroupResponse toDayGroupResponse(PricingDayGroup dayGroup);

    MatrixTimeBandResponse toTimeBandResponse(PricingTimeBand timeBand);

    @Mapping(target = "dayGroupCode", source = "dayGroup.code")
    @Mapping(target = "timeBandCode", source = "timeBand.code")
    @Mapping(
            target = "amountVnd",
            expression = "java(Long.toString(rule.getAmountVnd()))")
    MatrixFareRuleResponse toFareRuleResponse(FareRule rule);

    @Named("orderedMatrixDays")
    default List<DayOfWeek> orderedMatrixDays(Set<DayOfWeek> days) {
        return days.stream().sorted().toList();
    }
}
