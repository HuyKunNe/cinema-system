package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.FareSurchargeUnit;
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
        name = "fare_surcharge_rules",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fare_surcharges_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class FareSurchargeRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "fare_rule_code", nullable = false, length = 64)
    private String fareRuleCode;

    @Column(name = "movie_attribute_code", length = 64)
    private String movieAttributeCode;

    @Column(name = "required_eligibility_code", length = 64)
    private String requiredEligibilityCode;

    @Column(name = "amount_vnd", nullable = false)
    private long amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_unit", nullable = false, length = 30)
    private FareSurchargeUnit unit;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    protected FareSurchargeRule() {}

    public FareSurchargeRule(
            CinemaPricingPolicy policy,
            String code,
            String fareRuleCode,
            String movieAttributeCode,
            String requiredEligibilityCode,
            Long amountVnd,
            FareSurchargeUnit unit,
            int priority,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        if (policy == null
                || policy.getId() == null
                || amountVnd == null
                || amountVnd < 0
                || unit == null
                || priority < 0) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!"VND".equals(policy.getCurrency())) {
            throw invalid();
        }

        validatePeriod(policy, effectiveFrom, effectiveTo);

        this.policy = policy;
        this.code = normalize(code);
        this.fareRuleCode = normalize(fareRuleCode);
        this.movieAttributeCode =
                movieAttributeCode == null ? null : normalize(movieAttributeCode);
        this.requiredEligibilityCode =
                requiredEligibilityCode == null
                        ? null
                        : normalize(requiredEligibilityCode);
        this.amountVnd = amountVnd;
        this.unit = unit;
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

    public String getFareRuleCode() {
        return fareRuleCode;
    }

    public String getMovieAttributeCode() {
        return movieAttributeCode;
    }

    public String getRequiredEligibilityCode() {
        return requiredEligibilityCode;
    }

    public long getAmountVnd() {
        return amountVnd;
    }

    public FareSurchargeUnit getUnit() {
        return unit;
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
            CinemaPricingPolicy policy,
            LocalDate from,
            LocalDate to) {

        if (from == null
                || from.isBefore(policy.getEffectiveFrom())
                || (to != null && !to.isAfter(from))) {
            throw invalid();
        }

        LocalDate policyEnd = policy.getEffectiveTo();

        if (policyEnd != null
                && (!from.isBefore(policyEnd)
                        || to == null
                        || to.isAfter(policyEnd))) {
            throw invalid();
        }
    }

    private static String normalize(String value) {
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
        return new ValidationException(
                InventoryErrorCode.FARE_SURCHARGE_DATA_INVALID);
    }
}
