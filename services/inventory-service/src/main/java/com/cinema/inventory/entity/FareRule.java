package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareUnit;
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
import java.util.Locale;

@Entity
@Table(
        name = "fare_rules",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fare_rules_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class FareRule extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_group_id", nullable = false, updatable = false)
    private PricingDayGroup dayGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_band_id", nullable = false, updatable = false)
    private PricingTimeBand timeBand;

    @Column(name = "ticket_category_code", nullable = false, length = 64)
    private String ticketCategoryCode;

    @Column(name = "seat_type_code", nullable = false, length = 64)
    private String seatTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "advance_screening_condition", nullable = false, length = 30)
    private AdvanceScreeningCondition advanceScreeningCondition;

    @Column(name = "required_eligibility_code", length = 64)
    private String requiredEligibilityCode;

    @Column(name = "amount_vnd", nullable = false)
    private long amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "fare_unit", nullable = false, length = 20)
    private FareUnit fareUnit;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    protected FareRule() {}

    public FareRule(
            CinemaPricingPolicy policy,
            String code,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            PricingDayGroup dayGroup,
            PricingTimeBand timeBand,
            String ticketCategoryCode,
            String seatTypeCode,
            AdvanceScreeningCondition advanceScreeningCondition,
            String requiredEligibilityCode,
            Long amountVnd,
            FareUnit fareUnit,
            int priority,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        if (policy == null
                || experienceType == null
                || contentFormat == null
                || dayGroup == null
                || timeBand == null
                || advanceScreeningCondition == null
                || amountVnd == null
                || amountVnd < 0
                || fareUnit == null
                || priority < 0) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        // This model currently stores integer VND amounts only.
        if (!"VND".equals(policy.getCurrency())) {
            throw invalid();
        }

        if (!policy.getId().equals(dayGroup.getPolicy().getId())
                || !policy.getId().equals(timeBand.getPolicy().getId())) {
            throw invalid();
        }

        validatePeriod(policy, effectiveFrom, effectiveTo);

        this.policy = policy;
        this.code = normalizeCode(code);
        this.experienceType = experienceType;
        this.contentFormat = contentFormat;
        this.dayGroup = dayGroup;
        this.timeBand = timeBand;
        this.ticketCategoryCode = normalizeCode(ticketCategoryCode);
        this.seatTypeCode = normalizeCode(seatTypeCode);
        this.advanceScreeningCondition = advanceScreeningCondition;
        this.requiredEligibilityCode =
                requiredEligibilityCode == null ? null : normalizeCode(requiredEligibilityCode);
        this.amountVnd = amountVnd;
        this.fareUnit = fareUnit;
        this.priority = priority;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public PricingDayGroup getDayGroup() {
        return dayGroup;
    }

    public PricingTimeBand getTimeBand() {
        return timeBand;
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

    public String getRequiredEligibilityCode() {
        return requiredEligibilityCode;
    }

    public long getAmountVnd() {
        return amountVnd;
    }

    public FareUnit getFareUnit() {
        return fareUnit;
    }

    public int getPriority() {
        return priority;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    private static void validatePeriod(
            CinemaPricingPolicy policy, LocalDate effectiveFrom, LocalDate effectiveTo) {

        if (effectiveFrom == null
                || effectiveFrom.isBefore(policy.getEffectiveFrom())
                || (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom))) {
            throw invalid();
        }

        LocalDate policyEnd = policy.getEffectiveTo();

        if (policyEnd != null
                && (!effectiveFrom.isBefore(policyEnd)
                        || effectiveTo == null
                        || effectiveTo.isAfter(policyEnd))) {
            throw invalid();
        }
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.FARE_RULE_DATA_INVALID);
    }
}
