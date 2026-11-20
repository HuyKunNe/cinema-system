package com.cinema.user.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "staff_cinema_assignments")
public class StaffCinemaAssignment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "cinema_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID cinemaId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "assigned_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assignedBy;

    protected StaffCinemaAssignment() {}

    public StaffCinemaAssignment(User user, UUID cinemaId, UUID actor) {
        this.user = user;
        reassign(cinemaId, actor);
    }

    public void reassign(UUID cinemaId, UUID actor) {
        this.cinemaId = cinemaId;
        this.assignedBy = actor;
    }

    public User getUser() {
        return user;
    }

    public UUID getCinemaId() {
        return cinemaId;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }
}
