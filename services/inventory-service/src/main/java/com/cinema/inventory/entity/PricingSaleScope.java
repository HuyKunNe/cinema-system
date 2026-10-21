package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
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

import java.util.Locale;

@Entity
@Table(
        name = "pricing_sale_scopes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_sale_scopes_policy_code",
                    columnNames = {"policy_id", "code"}),
            @UniqueConstraint(
                    name = "uk_pricing_sale_scopes_dimensions",
                    columnNames = {
                        "policy_id",
                        "experience_type",
                        "content_format",
                        "day_group_code",
                        "time_band_code",
                        "ticket_category_code",
                        "seat_type_code",
                        "advance_screening_condition"
                    })
        })
public class PricingSaleScope extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 30)
    private ExperienceType experienceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 20)
    private ContentFormat contentFormat;

    @Column(name = "day_group_code", nullable = false, length = 64)
    private String dayGroupCode;

    @Column(name = "time_band_code", nullable = false, length = 64)
    private String timeBandCode;

    @Column(name = "ticket_category_code", nullable = false, length = 64)
    private String ticketCategoryCode;

    @Column(name = "seat_type_code", nullable = false, length = 64)
    private String seatTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "advance_screening_condition", nullable = false, length = 30)
    private AdvanceScreeningCondition advanceScreeningCondition;

    protected PricingSaleScope() {}

    public PricingSaleScope(
            CinemaPricingPolicy policy,
            String code,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            String dayGroupCode,
            String timeBandCode,
            String ticketCategoryCode,
            String seatTypeCode,
            AdvanceScreeningCondition advanceScreeningCondition) {

        if (policy == null
                || policy.getId() == null
                || experienceType == null
                || contentFormat == null
                || advanceScreeningCondition == null) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        this.policy = policy;
        this.code = normalize(code, false);
        this.experienceType = experienceType;
        this.contentFormat = contentFormat;
        this.dayGroupCode = normalize(dayGroupCode, true);
        this.timeBandCode = normalize(timeBandCode, true);
        this.ticketCategoryCode = normalize(ticketCategoryCode, false);
        this.seatTypeCode = normalize(seatTypeCode, false);
        this.advanceScreeningCondition = advanceScreeningCondition;
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public String getCode() {
        return code;
    }

    public ExperienceType getExperienceType() {
        return experienceType;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    public String getDayGroupCode() {
        return dayGroupCode;
    }

    public String getTimeBandCode() {
        return timeBandCode;
    }

    public String getTicketCategoryCode() {
        return ticketCategoryCode;
    }

    public String getSeatTypeCode() {
        return seatTypeCode;
    }

    public AdvanceScreeningCondition getAdvanceScreeningCondition() {
        return advanceScreeningCondition;
    }

    private static String normalize(String value, boolean allowLeadingDigit) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String pattern = allowLeadingDigit ? "[A-Z0-9][A-Z0-9_]{0,63}" : "[A-Z][A-Z0-9_]{0,63}";

        if (!normalized.matches(pattern)) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.PRICING_SALE_SCOPE_DATA_INVALID);
    }
}
