package com.cinema.inventory.dto.response;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        description =
                "Stored pricing seat definitions and physical seat mappings "
                        + "for one policy. Does not validate prices, fare units "
                        + "or publication readiness.")
public record CinemaPricingSeatTypesResponse(
        CinemaPricingPolicyResponse policy, List<PricingSeatTypeResponse> seatTypes) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PricingSeatTypeResponse(
            UUID id,
            String code,
            String name,
            PricingSeatMappingStatus mappingStatus,
            @Schema(nullable = true) SeatType physicalSeatType) {}
}
