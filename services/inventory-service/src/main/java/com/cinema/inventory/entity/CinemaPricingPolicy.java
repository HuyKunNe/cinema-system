package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingPolicyModel;
import com.cinema.inventory.enums.PricingPolicyStatus;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Locale;

@Entity
@Table(
        name = "cinema_pricing_policies",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_policies_cinema_version",
                    columnNames = {"cinema_id", "policy_version"})
        })
public class CinemaPricingPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false, updatable = false)
    private Cinema cinema;

    @Column(name = "policy_version", nullable = false, updatable = false)
    private long policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PricingPolicyStatus status = PricingPolicyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, updatable = false, length = 30)
    private PricingPolicyModel pricingModel = PricingPolicyModel.BASE_RULES_V1;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "time_zone", nullable = false, updatable = false, length = 64)
    private String timeZone;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "content_revision", nullable = false)
    private long contentRevision = 0L;

    protected CinemaPricingPolicy() {}

    public CinemaPricingPolicy(
            Cinema cinema,
            long policyVersion,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        this(
                cinema,
                policyVersion,
                currency,
                effectiveFrom,
                effectiveTo,
                PricingPolicyModel.BASE_RULES_V1);
    }

    public CinemaPricingPolicy(
            Cinema cinema,
            long policyVersion,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            PricingPolicyModel pricingModel) {

        if (cinema == null
                || cinema.getId() == null
                || policyVersion <= 0
                || pricingModel == null) {
            throw invalid();
        }

        validatePeriod(effectiveFrom, effectiveTo);

        this.cinema = cinema;
        this.policyVersion = policyVersion;
        this.currency = normalizeCurrency(currency);

        // Preserve the time-zone interpretation of this policy version.
        this.timeZone = cinema.requireTimeZone().getId();

        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = PricingPolicyStatus.DRAFT;
        this.pricingModel = pricingModel;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public PricingPolicyStatus getStatus() {
        return status;
    }

    public PricingPolicyModel getPricingModel() {
        return pricingModel;
    }

    public void requireBaseRulesModel() {
        if (pricingModel != PricingPolicyModel.BASE_RULES_V1) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_MODEL_UNSUPPORTED);
        }
    }

    public String getCurrency() {
        return currency;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void changeEffectivePeriod(LocalDate effectiveFrom, LocalDate effectiveTo) {

        requireDraft();
        validatePeriod(effectiveFrom, effectiveTo);

        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public void requireDraft() {
        if (status != PricingPolicyStatus.DRAFT) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_DRAFT);
        }
    }

    public void markContentChanged() {
        requireDraft();

        if (contentRevision == Long.MAX_VALUE) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_CONTENT_REVISION_EXHAUSTED);
        }

        contentRevision += 1;
    }

    private static void validatePeriod(LocalDate effectiveFrom, LocalDate effectiveTo) {

        if (effectiveFrom == null || (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom))) {
            throw invalid();
        }
    }

    private static String normalizeCurrency(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (normalized.length() != 3) {
            throw invalid();
        }

        try {
            return Currency.getInstance(normalized).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.PRICING_POLICY_DATA_INVALID);
    }
}
