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
    @JoinColumn(
            name = "creation_cinema_id",
            nullable = false,
            updatable = false)
    private Cinema creationCinema;

    @Column(
            name = "client_request_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID clientRequestId;

    @Column(
            name = "creation_fingerprint",
            nullable = false,
            updatable = false,
            length = 64)
    private String creationFingerprint;

    @Column(
            name = "creation_scope_fingerprint",
            nullable = false,
            updatable = false,
            length = 64)
    private String creationScopeFingerprint;

    protected CinemaPricingPolicy() {}

    public CinemaPricingPolicy(
            Cinema creationCinema,
            UUID clientRequestId,
            String creationFingerprint,
            String creationScopeFingerprint) {

        if (creationCinema == null
                || creationCinema.getId() == null
                || clientRequestId == null
                || !validFingerprint(creationFingerprint)
                || !validFingerprint(creationScopeFingerprint)) {

            throw new ValidationException(
                    InventoryErrorCode.INVALID_PRICING_POLICY);
        }

        this.creationCinema = creationCinema;
        this.clientRequestId = clientRequestId;
        this.creationFingerprint = creationFingerprint;
        this.creationScopeFingerprint = creationScopeFingerprint;
    }

    public Cinema getCreationCinema() {
        return creationCinema;
    }

    public UUID getClientRequestId() {
        return clientRequestId;
    }

    public String getCreationFingerprint() {
        return creationFingerprint;
    }

    public String getCreationScopeFingerprint() {
        return creationScopeFingerprint;
    }

    private static boolean validFingerprint(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
