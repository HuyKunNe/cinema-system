package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.service.pricing.BasePriceRuleSelector.Status;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record DraftBasePriceCoverageResponse(
        CinemaPricingPolicyResponse policy,
        boolean simulation,
        boolean publicationValidated,
        LocalDate localDate,
        PricingDayType dayType,
        TicketCategory ticketCategory,
        int fromMinute,
        int toMinute,
        boolean hasGaps,
        boolean hasAmbiguities,
        boolean segmentsTruncated,
        List<Segment> segments) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Segment(
            int fromMinute,
            int toMinute,
            Status status,
            BigDecimal baseAmount,
            List<RuleReference> winningRules) {}

    public record RuleReference(
            UUID id,
            String code,
            int priority,
            BigDecimal amount) {}
}
