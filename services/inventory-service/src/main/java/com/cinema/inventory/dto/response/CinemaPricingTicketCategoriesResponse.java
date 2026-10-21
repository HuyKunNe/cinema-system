package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.TicketEligibilityMode;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        description =
                "Stored ticket category definitions for one pricing policy. "
                        + "Does not verify customer eligibility or publication readiness.")
public record CinemaPricingTicketCategoriesResponse(
        CinemaPricingPolicyResponse policy,
        List<PricingTicketCategoryResponse> ticketCategories) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PricingTicketCategoryResponse(
            UUID id,
            String code,
            String name,
            TicketEligibilityMode eligibilityMode,
            @Schema(nullable = true)
                    String eligibilityRuleCode) {}
}
