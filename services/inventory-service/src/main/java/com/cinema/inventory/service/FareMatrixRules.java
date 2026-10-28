package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FareMatrixRules {

    private FareMatrixRules() {}

    public static void validateUniformFares(PricingDayGroup dayGroup, Map<Key, Value> values) {

        Map<PremiumGroup, Value> premiumGroups = new HashMap<>();
        Map<CoupleGroup, Value> coupleGroups = new HashMap<>();

        for (Value value : values.values()) {
            if (value.state() != FareCellState.PRICED) {
                continue;
            }

            Key key = value.key();

            // COUPLE has its own customer-group pricing rule.
            if (key.experienceType() == PricingExperienceType.PREMIUM_CINEMA
                    && key.seatType() != SeatType.COUPLE) {

                PremiumGroup group =
                        new PremiumGroup(key.contentFormat(), key.timeBandId(), key.seatType());

                requireSameFare(
                        premiumGroups,
                        group,
                        value,
                        InventoryErrorCode.PREMIUM_CATEGORY_FARE_CONFLICT);
            }

            if (key.seatType() == SeatType.COUPLE && dayGroup != PricingDayGroup.HOLIDAY) {

                CoupleGroup group =
                        new CoupleGroup(
                                key.experienceType(), key.contentFormat(), key.ticketGroupId());

                requireSameFare(
                        coupleGroups, group, value, InventoryErrorCode.COUPLE_FIXED_FARE_CONFLICT);
            }
        }
    }

    private static <K> void requireSameFare(
            Map<K, Value> groups, K group, Value candidate, InventoryErrorCode errorCode) {

        Value previous = groups.putIfAbsent(group, candidate);

        if (previous != null && !previous.sameFare(candidate)) {
            throw new ValidationException(errorCode);
        }
    }

    private record PremiumGroup(ContentFormat contentFormat, UUID timeBandId, SeatType seatType) {}

    private record CoupleGroup(
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            UUID ticketGroupId) {}
}

