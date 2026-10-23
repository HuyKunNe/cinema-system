package com.cinema.inventory.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "ticket_eligibility_approvals")
public class TicketEligibilityApproval extends BaseEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "user_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "show_seat_id",
            nullable = false,
            updatable = false)
    private ShowSeat showSeat;

    @Column(
            name = "ticket_category_code",
            nullable = false,
            updatable = false,
            length = 64)
    private String ticketCategoryCode;

    @Column(
            name = "eligibility_rule_code",
            nullable = false,
            updatable = false,
            length = 64)
    private String eligibilityRuleCode;

    @Column(
            name = "attendee_name",
            nullable = false,
            updatable = false,
            length = 150)
    private String attendeeName;

    @Column(
            name = "verification_reference",
            nullable = false,
            updatable = false,
            length = 120)
    private String verificationReference;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "verified_by",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID verifiedBy;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "valid_until", nullable = false, updatable = false)
    private OffsetDateTime validUntil;

    protected TicketEligibilityApproval() {}

    public TicketEligibilityApproval(
            UUID userId,
            ShowSeat showSeat,
            String ticketCategoryCode,
            String eligibilityRuleCode,
            String attendeeName,
            String verificationReference,
            UUID verifiedBy,
            OffsetDateTime verifiedAt,
            OffsetDateTime validUntil) {

        this.userId = userId;
        this.showSeat = showSeat;
        this.ticketCategoryCode = ticketCategoryCode;
        this.eligibilityRuleCode = eligibilityRuleCode;
        this.attendeeName = attendeeName;
        this.verificationReference = verificationReference;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
        this.validUntil = validUntil;
    }

    public UUID getUserId() {
        return userId;
    }

    public ShowSeat getShowSeat() {
        return showSeat;
    }

    public String getTicketCategoryCode() {
        return ticketCategoryCode;
    }

    public String getEligibilityRuleCode() {
        return eligibilityRuleCode;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public String getVerificationReference() {
        return verificationReference;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }
}
