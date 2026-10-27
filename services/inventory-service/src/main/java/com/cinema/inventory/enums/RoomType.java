package com.cinema.inventory.enums;

public enum RoomType {
    STANDARD,
    IMAX,
    FOUR_DX,
    SCREEN_X,
    VIP;

    public PricingExperienceType getPricingExperienceType() {
        return switch (this) {
            case STANDARD -> PricingExperienceType.STANDARD;
            case IMAX -> PricingExperienceType.IMAX;
            case FOUR_DX -> PricingExperienceType.FOUR_DX;
            case SCREEN_X -> PricingExperienceType.SCREEN_X;
            case VIP -> PricingExperienceType.PREMIUM_CINEMA;
        };
    }
}
