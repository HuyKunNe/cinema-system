package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record BasePriceEvaluationContext(
        OffsetDateTime startsAt,
        TicketCategory ticketCategory,
        Set<String> memberTiers,
        String channel) {

    public BasePriceEvaluationContext {
        if (startsAt == null || ticketCategory == null) {
            throw invalid();
        }

        Set<String> normalizedTiers = new HashSet<>();

        if (memberTiers != null) {
            for (String tier : memberTiers) {
                normalizedTiers.add(normalizeCode(tier));
            }
        }

        memberTiers = Set.copyOf(normalizedTiers);
        channel = channel == null ? null : normalizeCode(channel);
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
    }
}
