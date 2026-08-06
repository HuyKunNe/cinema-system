package com.cinema.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class AuthorityCatalogTest {

    @Test
    void roleCatalogShouldContainApprovedRoles() {
        assertThat(RoleName.values())
                .containsExactly(
                        RoleName.USER,
                        RoleName.STAFF,
                        RoleName.ADMIN,
                        RoleName.SERVICE);
    }

    @Test
    void roleNamesShouldNotContainSpringPrefix() {
        assertThat(RoleName.values())
                .allMatch(role -> !role.name().startsWith("ROLE_"));
    }

    @Test
    void permissionCodesShouldBeUnique() {
        assertThat(
                Arrays.stream(PermissionCode.values())
                        .map(PermissionCode::getCode))
                .doesNotHaveDuplicates();
    }

    @Test
    void permissionCodesShouldFollowCanonicalFormat() {
        assertThat(
                Arrays.stream(PermissionCode.values())
                        .map(PermissionCode::getCode))
                .allMatch(code -> code.matches(
                        "[a-z][a-z0-9-]*:[a-z][a-z0-9-]*"));
    }

    @Test
    void permissionCatalogShouldContainApprovedPermissions() {
        assertThat(
                Arrays.stream(PermissionCode.values())
                        .map(PermissionCode::getCode))
                .containsExactly(
                        "booking:create",
                        "booking:read",
                        "booking:cancel",
                        "movie:manage",
                        "showtime:manage",
                        "inventory:manage",
                        "payment:read",
                        "notification:manage",
                        "user:manage");
    }
}
