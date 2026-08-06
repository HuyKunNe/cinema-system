package com.cinema.user.entity.id;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserRoleId implements Serializable {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "role_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID roleId;

    protected UserRoleId() {
    }

    public UserRoleId(
            UUID userId,
            UUID roleId) {

        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof UserRoleId other)) {
            return false;
        }

        return Objects.equals(userId, other.userId)
                && Objects.equals(roleId, other.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
