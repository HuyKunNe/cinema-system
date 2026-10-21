package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(
        name = "pricing_day_groups",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pricing_day_groups_policy_code",
                    columnNames = {"policy_id", "code"})
        })
public class PricingDayGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "pricing_day_group_days",
            joinColumns = @JoinColumn(name = "day_group_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(name = "includes_holidays", nullable = false)
    private boolean includesHolidays;

    protected PricingDayGroup() {}

    public PricingDayGroup(
            CinemaPricingPolicy policy,
            String code,
            String name,
            Set<DayOfWeek> daysOfWeek,
            boolean includesHolidays) {

        if (policy == null
                || daysOfWeek == null
                || daysOfWeek.stream().anyMatch(day -> day == null)
                || (daysOfWeek.isEmpty() && !includesHolidays)) {
            throw invalid();
        }

        policy.requireFareMatrixModel();
        policy.requireDraft();

        this.policy = policy;
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.daysOfWeek = new HashSet<>(daysOfWeek);
        this.includesHolidays = includesHolidays;
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return Set.copyOf(daysOfWeek);
    }

    public boolean isIncludesHolidays() {
        return includesHolidays;
    }

    public boolean matches(LocalDate localDate, boolean holiday) {
        if (localDate == null) {
            throw invalid();
        }

        // Holiday classification overrides the weekday.
        if (holiday) {
            return includesHolidays;
        }

        return daysOfWeek.contains(localDate.getDayOfWeek());
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z0-9][A-Z0-9_]{0,63}")) {
            throw invalid();
        }

        return normalized;
    }

    private static String normalizeName(String value) {
        if (value == null) {
            throw invalid();
        }

        String normalized = value.trim();

        if (normalized.isEmpty() || normalized.length() > 150) {
            throw invalid();
        }

        return normalized;
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.PRICING_DIMENSION_DATA_INVALID);
    }
}
