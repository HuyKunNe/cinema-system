package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class FareMatrixScope {

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 30)
    private PricingExperienceType experienceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 20)
    private ContentFormat contentFormat;

    protected FareMatrixScope() {}

    public FareMatrixScope(PricingExperienceType experienceType, ContentFormat contentFormat) {

        if (experienceType == null || contentFormat == null) {
            throw new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
        }

        this.experienceType = experienceType;
        this.contentFormat = contentFormat;
    }

    public PricingExperienceType getExperienceType() {
        return experienceType;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof FareMatrixScope scope
                && experienceType == scope.experienceType
                && contentFormat == scope.contentFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(experienceType, contentFormat);
    }
}
