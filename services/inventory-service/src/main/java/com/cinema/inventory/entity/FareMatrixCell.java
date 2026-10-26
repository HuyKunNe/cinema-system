package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "pricing_fare_cells")
public class FareMatrixCell extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_version_id", nullable = false, updatable = false)
    private CinemaPricingPolicyVersion policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 30)
    private PricingExperienceType experienceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 20)
    private ContentFormat contentFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_band", nullable = false, length = 20)
    private PricingTimeBand timeBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_category", nullable = false, length = 30)
    private TicketCategory ticketCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 24)
    private FareCellState state;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    protected FareMatrixCell() {}

    public FareMatrixCell(
            CinemaPricingPolicyVersion policyVersion,
            Value value) {

        if (policyVersion == null
                || policyVersion.getId() == null
                || value == null
                || value.state() == FareCellState.UNCONFIGURED) {
            throw invalid();
        }

        policyVersion.requireDraft();

        this.policyVersion = policyVersion;
        this.experienceType = value.key().experienceType();
        this.contentFormat = value.key().contentFormat();
        this.timeBand = value.key().timeBand();
        this.ticketCategory = value.key().ticketCategory();
        this.seatType = value.key().seatType();
        this.state = value.state();
        this.amount = value.amount();
    }

    public Value toValue() {
        return new Value(
                new Key(
                        experienceType,
                        contentFormat,
                        timeBand,
                        ticketCategory,
                        seatType),
                state,
                amount);
    }

    public record Key(
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            PricingTimeBand timeBand,
            TicketCategory ticketCategory,
            SeatType seatType) {

        public Key {
            if (experienceType == null
                    || contentFormat == null
                    || timeBand == null
                    || ticketCategory == null
                    || seatType == null) {
                throw invalid();
            }
        }
    }

    public record Value(
            Key key,
            FareCellState state,
            BigDecimal amount) {

        public Value {
            if (key == null || state == null) {
                throw invalid();
            }

            if (state == FareCellState.PRICED) {
                if (amount == null || amount.signum() < 0) {
                    throw invalid();
                }

                try {
                    amount = amount.setScale(2, RoundingMode.UNNECESSARY);
                } catch (ArithmeticException exception) {
                    throw invalid();
                }

                if (amount.precision() > 12) {
                    throw invalid();
                }
            } else if (amount != null) {
                throw invalid();
            }
        }

        public boolean sameFare(Value other) {
            return state == other.state
                    && Objects.equals(amount, other.amount);
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.INVALID_FARE_MATRIX);
    }
}
