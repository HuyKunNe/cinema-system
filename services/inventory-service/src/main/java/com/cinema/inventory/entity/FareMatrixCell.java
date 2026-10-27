package com.cinema.inventory.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "time_band_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID timeBandId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "ticket_group_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID ticketGroupId;

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

        Key key = value.key();

        boolean hasTimeBand =
                policyVersion.getPricingTimeBands().stream()
                        .anyMatch(item -> item.getId().equals(key.timeBandId()));

        boolean hasTicketGroup =
                policyVersion.getPricingTicketGroups().stream()
                        .anyMatch(item -> item.getId().equals(key.ticketGroupId()));

        if (!hasTimeBand || !hasTicketGroup) {
            throw invalid();
        }

        this.policyVersion = policyVersion;
        this.experienceType = key.experienceType();
        this.contentFormat = key.contentFormat();
        this.timeBandId = key.timeBandId();
        this.ticketGroupId = key.ticketGroupId();
        this.seatType = key.seatType();
        this.state = value.state();
        this.amount = value.amount();
    }

    public UUID getTimeBandId() {
        return timeBandId;
    }

    public UUID getTicketGroupId() {
        return ticketGroupId;
    }

        public Value toValue() {
        boolean hasTimeBand =
                policyVersion.getPricingTimeBands().stream()
                        .anyMatch(item -> item.getId().equals(timeBandId));

        boolean hasTicketGroup =
                policyVersion.getPricingTicketGroups().stream()
                        .anyMatch(item -> item.getId().equals(ticketGroupId));

        if (!hasTimeBand || !hasTicketGroup) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
        }

        return new Value(
                new Key(
                        experienceType,
                        contentFormat,
                        timeBandId,
                        ticketGroupId,
                        seatType),
                state,
                amount);
    }

        public record Key(
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            UUID timeBandId,
            UUID ticketGroupId,
            SeatType seatType) {

        public Key {
            if (experienceType == null
                    || contentFormat == null
                    || timeBandId == null
                    || ticketGroupId == null
                    || seatType == null) {

                throw invalid();
            }
        }
    }
    public record Value(Key key, FareCellState state, BigDecimal amount) {

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
            return state == other.state && Objects.equals(amount, other.amount);
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
    }
}
