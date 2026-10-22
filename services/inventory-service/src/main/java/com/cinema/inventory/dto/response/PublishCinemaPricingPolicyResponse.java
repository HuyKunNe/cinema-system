package com.cinema.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PublishCinemaPricingPolicyResponse(
        boolean published,
        CinemaPricingPolicyResponse policy,
        ValidateDraftFareMatrixResponse prePublicationValidation) {}
