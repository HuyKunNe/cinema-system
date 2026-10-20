package com.cinema.inventory.dto.response;

import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse.HolidayDateResponse;
import com.cinema.inventory.dto.response.CinemaSurchargesResponse.SurchargeResponse;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.TicketCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyContentResponse(
        CinemaPricingPolicyResponse policy,
        List<RuleResponse> rules,
        List<HolidayDateResponse> holidayDates,
        List<SurchargeResponse> surcharges) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RuleResponse(
            UUID id,
            String code,
            TicketCategory ticketCategory,
            PricingDayType dayType,
            List<DayOfWeek> daysOfWeek,
            int fromMinute,
            int toMinute,
            BigDecimal amount,
            int priority,
            String requiredMemberTier,
            String requiredChannel) {}
}
