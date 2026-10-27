package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "pricing_time_bands")
public class PricingTimeBandDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_version_id", nullable = false, updatable = false)
    private CinemaPricingPolicyVersion policyVersion;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    @Column(name = "end_minute", nullable = false)
    private int endMinute;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_code", length = 20, updatable = false)
    private PricingTimeBand legacyCode;

    protected PricingTimeBandDefinition() {}

    public PricingTimeBandDefinition(
            CinemaPricingPolicyVersion policyVersion,
            String name,
            int startMinute,
            int endMinute,
            int sortOrder) {

        this(policyVersion, name, startMinute, endMinute, sortOrder, null);
    }

    private PricingTimeBandDefinition(
            CinemaPricingPolicyVersion policyVersion,
            String name,
            int startMinute,
            int endMinute,
            int sortOrder,
            PricingTimeBand legacyCode) {

        if (policyVersion == null || policyVersion.getId() == null) {
            throw invalid();
        }

        this.policyVersion = policyVersion;
        this.legacyCode = legacyCode;

        replaceDraftDetails(new Details(name, startMinute, endMinute, sortOrder));
    }

    public static PricingTimeBandDefinition fromLegacy(
            CinemaPricingPolicyVersion owner, PricingTimeBand code) {

        if (code == null) {
            throw invalid();
        }

        return switch (code) {
            case BEFORE_17 -> new PricingTimeBandDefinition(owner, "Trước 17:00", 0, 1020, 0, code);
            case FROM_17 -> new PricingTimeBandDefinition(owner, "Từ 17:00", 1020, 1440, 1, code);
        };
    }

    public PricingTimeBandDefinition copyTo(CinemaPricingPolicyVersion owner) {

        return new PricingTimeBandDefinition(
                owner, name, startMinute, endMinute, sortOrder, legacyCode);
    }

    public String getName() {
        return name;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public PricingTimeBand getLegacyCode() {
        return legacyCode;
    }

    public boolean contains(LocalTime time) {
        if (time == null) {
            throw invalid();
        }

        return containsMinute(time.getHour() * 60 + time.getMinute());
    }

    boolean containsMinute(int minute) {
        if (endMinute > startMinute) {
            return minute >= startMinute && minute < endMinute;
        }

        return minute >= startMinute || minute < endMinute;
    }

    boolean replaceDraftDetails(Details details) {
        policyVersion.requireDraft();

        if (details == null) {
            throw invalid();
        }

        boolean changed =
                !Objects.equals(name, details.name())
                        || startMinute != details.startMinute()
                        || endMinute != details.endMinute()
                        || sortOrder != details.sortOrder();

        this.name = details.name();
        this.startMinute = details.startMinute();
        this.endMinute = details.endMinute();
        this.sortOrder = details.sortOrder();

        return changed;
    }

    public record Details(String name, int startMinute, int endMinute, int sortOrder) {

        public Details {
            if (name == null
                    || startMinute < 0
                    || startMinute >= 1440
                    || endMinute < 0
                    || endMinute > 1440
                    || startMinute == endMinute
                    || sortOrder < 0) {

                throw invalid();
            }

            name = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);

            if (name.isEmpty() || name.length() > 120) {
                throw invalid();
            }

            endMinute = endMinute == 0 ? 1440 : endMinute;
        }

        boolean containsMinute(int minute) {
            if (endMinute > startMinute) {
                return minute >= startMinute && minute < endMinute;
            }

            return minute >= startMinute || minute < endMinute;
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
    }
}
