package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.TicketCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaPricingPolicyReviewResponse(
        CinemaPricingPolicyResponse policy,
        boolean hasErrors,
        boolean reportTruncated,
        boolean publicationValidated,
        List<Issue> issues,
        List<String> uncheckedAreas) {

    public enum Severity {
        ERROR,
        WARNING
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Issue(
            Severity severity,
            String code,
            String message,
            TicketCategory ticketCategory,
            List<UUID> relatedIds) {}
}
