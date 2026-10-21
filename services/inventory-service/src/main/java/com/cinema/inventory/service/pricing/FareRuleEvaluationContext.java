package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record FareRuleEvaluationContext(
        UUID policyId,
        OffsetDateTime localStartsAt,
        boolean holiday,
        ExperienceType experienceType,
        ContentFormat contentFormat,
        String ticketCategoryCode,
        String seatTypeCode,
        boolean firstShowing,
        Set<String> eligibilityCodes) {

    public FareRuleEvaluationContext {
        if (policyId == null
                || localStartsAt == null
                || experienceType == null
                || contentFormat == null) {
            throw invalid();
        }

        ticketCategoryCode = normalizeCode(ticketCategoryCode);
        seatTypeCode = normalizeCode(seatTypeCode);

        eligibilityCodes =
                eligibilityCodes == null
                        ? Set.of()
                        : eligibilityCodes.stream()
                                .map(FareRuleEvaluationContext::normalizeCode)
                                .collect(Collectors.toUnmodifiableSet());
    }

    public LocalDate localDate() {
        return localStartsAt.toLocalDate();
    }

    public int minuteOfDay() {
        return localStartsAt.getHour() * 60 + localStartsAt.getMinute();
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
        return new ValidationException(
                InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
    }
}
