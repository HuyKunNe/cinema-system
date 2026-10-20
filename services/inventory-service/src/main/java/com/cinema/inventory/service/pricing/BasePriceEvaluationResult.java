package com.cinema.inventory.service.pricing;

import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.TicketCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BasePriceEvaluationResult(
        CinemaPricingPolicyResponse policy,
        UUID ruleId,
        String ruleCode,
        TicketCategory ticketCategory,
        OffsetDateTime localStartsAt,
        PricingDayType dayType,
        int priority,
        BigDecimal baseAmount) {}
