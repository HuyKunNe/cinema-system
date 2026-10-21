package com.cinema.inventory.enums;

public enum TicketPricingRole {
    UNCONFIRMED,
    REGULAR,
    CATEGORY_DISCOUNT,
    C_MONDAY,
    C_MEMBER,
    C_TEN;

    public boolean excludedFromAdvanceScreening() {
        return this == C_MONDAY || this == C_MEMBER || this == C_TEN;
    }
}
