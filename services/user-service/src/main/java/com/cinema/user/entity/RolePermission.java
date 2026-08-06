package com.cinema.user.entity;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Persistable;

import com.cinema.user.entity.id.RolePermissionId;

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
@Table(name = "role_permissions")
public class RolePermission implements Persistable<RolePermissionId> {

    @EmbeddedId
    private RolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permissions_role"))
    private Role role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permissions_permission"))
    private Permission permission;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", foreignKey = @ForeignKey(name = "fk_role_permissions_assigned_by"))
    private User assignedBy;

    @Transient
    private boolean newEntity = true;

    protected RolePermission() {
    }

    public RolePermission(
            Role role,
            Permission permission,
            OffsetDateTime assignedAt,
            User assignedBy) {

        this.id = new RolePermissionId(
                role.getId(),
                permission.getId());

        this.role = role;
        this.permission = permission;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    @Override
    public RolePermissionId getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
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
