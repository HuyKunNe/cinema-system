package com.cinema.inventory.enums;

public enum PricingPolicyVersionStatus {
    DRAFT,
    PUBLISHED,
    RETIRED;

    public boolean isPublishedSnapshot() {
        return this == PUBLISHED || this == RETIRED;
    }
}
