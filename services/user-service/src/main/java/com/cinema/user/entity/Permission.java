package com.cinema.user.entity;

import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.enums.PermissionCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_code", columnNames = "code")
})
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    protected Permission() {
    }

    public Permission(
            PermissionCode permissionCode,
            String description) {

        this.code = permissionCode.getCode();
        this.description = description;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
