package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse;
import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse.HolidayDateResponse;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = InventoryMapperConfig.class, uses = CinemaPricingPolicyMapper.class)
public interface CinemaHolidayDatesMapper {

    @Mapping(target = "policy", source = "policy")
    @Mapping(target = "holidayDates", source = "holidayDates")
    CinemaHolidayDatesResponse toResponse(
            CinemaPricingPolicy policy, List<CinemaHolidayDate> holidayDates);

    @Mapping(target = "date", source = "holidayDate")
    HolidayDateResponse toHolidayDateResponse(CinemaHolidayDate holidayDate);
}
