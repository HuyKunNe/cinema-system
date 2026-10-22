package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

public record PricingPolicyPeriod(
        Instant fromInclusive,
        Instant toExclusive) {

    public static PricingPolicyPeriod from(CinemaPricingPolicy policy) {
        if (policy == null
                || policy.getEffectiveFrom() == null
                || policy.getTimeZone() == null
                || policy.getTimeZone().isBlank()) {
            throw invalid();
        }

        try {
            ZoneId zone = ZoneId.of(policy.getTimeZone());

            Instant from =
                    policy.getEffectiveFrom()
                            .atStartOfDay(zone)
                            .toInstant();

            Instant to =
                    policy.getEffectiveTo() == null
                            ? Instant.MAX
                            : policy.getEffectiveTo()
                                    .atStartOfDay(zone)
                                    .toInstant();

            if (!from.isBefore(to)) {
                throw invalid();
            }

            return new PricingPolicyPeriod(from, to);
        } catch (DateTimeException exception) {
            throw invalid();
        }
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(fromInclusive)
                && instant.isBefore(toExclusive);
    }

    public boolean overlaps(PricingPolicyPeriod other) {
        return fromInclusive.isBefore(other.toExclusive())
                && other.fromInclusive().isBefore(toExclusive);
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.PRICING_POLICY_DATA_INVALID);
    }
}
