package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FareMatrixRules {

    private FareMatrixRules() {}

    public static void validateUniformFares(Map<Key, Value> values) {

        Map<PremiumGroup, Value> premiumGroups = new HashMap<>();

        for (Value value : values.values()) {
            Key key = value.key();

            // Every COUPLE cell is independently configured.
            if (key.seatType() == SeatType.COUPLE
                    || value.state() != FareCellState.PRICED
                    || key.experienceType() != PricingExperienceType.PREMIUM_CINEMA) {

                continue;
            }

            PremiumGroup group =
                    new PremiumGroup(key.contentFormat(), key.timeBandId(), key.seatType());

            Value previous = premiumGroups.putIfAbsent(group, value);

            if (previous != null && !previous.sameFare(value)) {
                throw new ValidationException(InventoryErrorCode.PREMIUM_CATEGORY_FARE_CONFLICT);
            }
        }
    }

    private record PremiumGroup(ContentFormat contentFormat, UUID timeBandId, SeatType seatType) {}
}
