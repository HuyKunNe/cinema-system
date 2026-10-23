package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.enums.TicketPricingRole;
import com.cinema.inventory.exception.InventoryErrorCode;

public final class CategoryDiscountEligibilityGuard {

    private CategoryDiscountEligibilityGuard() {}

    public static void requireAllowed(
            PricingTicketCategory category,
            SeatType physicalSeatType) {

        if (category.getPricingRole()
                != TicketPricingRole.CATEGORY_DISCOUNT) {
            return;
        }

        if (physicalSeatType == SeatType.COUPLE) {
            throw new ConflictException(
                    InventoryErrorCode
                            .CATEGORY_DISCOUNT_COUPLE_NOT_ALLOWED);
        }

        if (category.getEligibilityMode()
                        != TicketEligibilityMode.RULE_REQUIRED
                || category.getEligibilityRuleCode() == null
                || category.getEligibilityRuleCode().isBlank()) {

            throw new ConflictException(
                    InventoryErrorCode
                            .PRICING_TICKET_CATEGORY_ELIGIBILITY_UNCONFIRMED);
        }
    }
}
