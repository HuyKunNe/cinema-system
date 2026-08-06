package com.cinema.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;

class AuthorityEntityTest {

    @Test
    void roleShouldUseApprovedRoleName() {
        Role role = new Role(
                RoleName.ADMIN,
                "Platform administrator");

        assertThat(role.getName())
                .isEqualTo(RoleName.ADMIN);

        assertThat(role.getDescription())
                .isEqualTo("Platform administrator");
    }

    @Test
    void permissionShouldPersistCanonicalCode() {
        Permission permission = new Permission(
                PermissionCode.INVENTORY_MANAGE,
                "Manage cinema inventory");

        assertThat(permission.getCode())
                .isEqualTo("inventory:manage");

        assertThat(permission.getDescription())
                .isEqualTo("Manage cinema inventory");
    }

    @Test
    void descriptionsShouldBeUpdatableWithoutChangingIdentity() {
        Role role = new Role(
                RoleName.STAFF,
                "Old role description");

        Permission permission = new Permission(
                PermissionCode.MOVIE_MANAGE,
                "Old permission description");

        role.updateDescription("Updated role description");
        permission.updateDescription(
                "Updated permission description");

        assertThat(role.getName())
                .isEqualTo(RoleName.STAFF);

        assertThat(role.getDescription())
                .isEqualTo("Updated role description");

        assertThat(permission.getCode())
                .isEqualTo("movie:manage");

        assertThat(permission.getDescription())
                .isEqualTo("Updated permission description");
    }
}
