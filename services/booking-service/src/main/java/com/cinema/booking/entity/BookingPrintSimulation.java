package com.cinema.booking.entity;

import com.cinema.booking.enums.PrintSimulationStatus;
import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "booking_print_simulations")
public class BookingPrintSimulation extends BaseEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "cinema_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID cinemaId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "device_id", nullable = false, updatable = false, length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private PrintSimulationStatus status;

    @Column(
            name = "response_json",
            nullable = false,
            updatable = false,
            columnDefinition = "LONGTEXT")
    private String responseJson;

    protected BookingPrintSimulation() {}

    public BookingPrintSimulation(
            UUID bookingId,
            UUID actorId,
            UUID cinemaId,
            String idempotencyKey,
            String deviceId,
            PrintSimulationStatus status) {
        this.bookingId = bookingId;
        this.actorId = actorId;
        this.cinemaId = cinemaId;
        this.idempotencyKey = idempotencyKey;
        this.deviceId = deviceId;
        this.status = status;
    }

    public void complete(String responseJson) {
        this.responseJson = responseJson;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getCinemaId() {
        return cinemaId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public PrintSimulationStatus getStatus() {
        return status;
    }

    public String getResponseJson() {
        return responseJson;
    }
}
