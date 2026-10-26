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

import java.util.UUID;

@Entity
@Table(name = "cinema_pricing_policies")
public class CinemaPricingPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false, updatable = false)
    private Cinema cinema;

    @Column(
            name = "client_request_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID clientRequestId;

    @Column(name = "creation_fingerprint", nullable = false, updatable = false, length = 64)
    private String creationFingerprint;

    protected CinemaPricingPolicy() {}

    public CinemaPricingPolicy(Cinema cinema, UUID clientRequestId, String creationFingerprint) {

        if (cinema == null
                || cinema.getId() == null
                || clientRequestId == null
                || creationFingerprint == null
                || !creationFingerprint.matches("[0-9a-f]{64}")) {
            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
        }

        this.cinema = cinema;
        this.clientRequestId = clientRequestId;
        this.creationFingerprint = creationFingerprint;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public UUID getClientRequestId() {
        return clientRequestId;
    }

    public String getCreationFingerprint() {
        return creationFingerprint;
    }
}
