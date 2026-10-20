package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SurchargeTarget;
import com.cinema.inventory.enums.SurchargeUnit;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaSurchargesResponse(
        CinemaPricingPolicyResponse policy,
        List<SurchargeResponse> surcharges) {

    public record SurchargeResponse(
            UUID id,
            String code,
            SurchargeTarget target,
            String targetCode,
            BigDecimal amount,
            SurchargeUnit unit,
            boolean stackable,
            int priority) {}
}
