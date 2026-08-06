package com.cinema.user.entity.id;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RolePermissionId implements Serializable {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "role_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID roleId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "permission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID permissionId;

    protected RolePermissionId() {
    }

    public RolePermissionId(
            UUID roleId,
            UUID permissionId) {

        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RolePermissionId other)) {
            return false;
        }

        return Objects.equals(roleId, other.roleId)
                && Objects.equals(permissionId, other.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permissionId);
    }
}
