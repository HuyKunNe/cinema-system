package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.util.HashMap;
import java.util.Map;

public final class FareMatrixRules {

    private FareMatrixRules() {}

    public static void validateUniformFares(PricingDayGroup dayGroup, Map<Key, Value> values) {

        Map<Key, Value> premiumGroups = new HashMap<>();
        Map<Key, Value> coupleGroups = new HashMap<>();

        for (Value value : values.values()) {
            Key key = value.key();

            if (key.experienceType() == PricingExperienceType.PREMIUM_CINEMA) {
                Key group =
                        new Key(
                                key.experienceType(),
                                key.contentFormat(),
                                key.timeBand(),
                                TicketCategory.ADULT,
                                key.seatType());

                requireSameFare(
                        premiumGroups,
                        group,
                        value,
                        InventoryErrorCode.PREMIUM_CATEGORY_FARE_CONFLICT);
            }

            if (dayGroup != PricingDayGroup.HOLIDAY && key.seatType() == SeatType.COUPLE) {

                Key group =
                        new Key(
                                key.experienceType(),
                                key.contentFormat(),
                                PricingTimeBand.BEFORE_17,
                                TicketCategory.ADULT,
                                SeatType.COUPLE);

                requireSameFare(
                        coupleGroups, group, value, InventoryErrorCode.COUPLE_FIXED_FARE_CONFLICT);
            }
        }
    }

    private static void requireSameFare(
            Map<Key, Value> groups, Key group, Value candidate, InventoryErrorCode errorCode) {

        Value previous = groups.putIfAbsent(group, candidate);

        if (previous != null && !previous.sameFare(candidate)) {
            throw new ValidationException(errorCode);
        }
    }
}
