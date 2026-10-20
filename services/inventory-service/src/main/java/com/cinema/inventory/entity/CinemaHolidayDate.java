package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "cinema_holiday_dates",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_cinema_holiday_dates_policy_date",
                    columnNames = {"policy_id", "holiday_date"})
        })
public class CinemaHolidayDate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private CinemaPricingPolicy policy;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    protected CinemaHolidayDate() {}

    public CinemaHolidayDate(CinemaPricingPolicy policy, LocalDate holidayDate, String name) {

        if (policy == null || holidayDate == null || name == null) {
            throw invalid();
        }

        policy.requireDraft();

        String normalizedName = name.trim();

        if (normalizedName.isEmpty() || normalizedName.length() > 150) {
            throw invalid();
        }

        if (holidayDate.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !holidayDate.isBefore(policy.getEffectiveTo()))) {
            throw invalid();
        }

        this.policy = policy;
        this.holidayDate = holidayDate;
        this.name = normalizedName;
    }

    public CinemaPricingPolicy getPolicy() {
        return policy;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getName() {
        return name;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.PRICING_POLICY_HOLIDAY_DATA_INVALID);
    }
}
