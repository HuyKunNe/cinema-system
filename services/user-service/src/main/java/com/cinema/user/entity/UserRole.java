package com.cinema.user.entity;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Persistable;

import com.cinema.user.entity.id.UserRoleId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "user_roles")
public class UserRole implements Persistable<UserRoleId> {

    @EmbeddedId
    private UserRoleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_user"))
    private User user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    private Role role;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", foreignKey = @ForeignKey(name = "fk_user_roles_assigned_by"))
    private User assignedBy;

    @Transient
    private boolean newEntity = true;

    protected UserRole() {
    }

    public UserRole(
            User user,
            Role role,
            OffsetDateTime assignedAt,
            User assignedBy) {

        this.id = new UserRoleId(
                user.getId(),
                role.getId());

        this.user = user;
        this.role = role;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public UserRoleId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.newEntity = false;
    }
}
