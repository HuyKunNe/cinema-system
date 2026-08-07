package com.cinema.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.Permission;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.RolePermission;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;

import jakarta.persistence.EntityManager;

@Transactional
class RolePermissionRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime ASSIGNED_AT = OffsetDateTime.parse("2026-08-01T03:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistRolePermissionWithCompositeId() {
        User assignedBy = createUser("persist-permission");
        Role serviceRole = findRole(RoleName.SERVICE);
        Permission permission = findPermission(
                PermissionCode.BOOKING_READ);

        RolePermission assignment = new RolePermission(
                serviceRole,
                permission,
                ASSIGNED_AT,
                assignedBy);

        RolePermission saved = rolePermissionRepository
                .saveAndFlush(assignment);

        assertThat(saved.getId().getRoleId())
                .isEqualTo(serviceRole.getId());

        assertThat(saved.getId().getPermissionId())
                .isEqualTo(permission.getId());

        assertThat(saved.getAssignedAt())
                .isEqualTo(ASSIGNED_AT);

        assertThat(saved.getAssignedBy().getId())
                .isEqualTo(assignedBy.getId());

        assertThat(saved.isNew()).isFalse();
    }

    @Test
    void shouldFindPermissionsByRoleId() {
        Role role = findRole(RoleName.USER);

        List<RolePermission> assignments = rolePermissionRepository
                .findAllByRole_Id(role.getId());

        assertThat(assignments)
                .extracting(assignment -> assignment.getPermission().getCode())
                .containsExactlyInAnyOrder(
                        "booking:create",
                        "booking:read",
                        "booking:cancel");
    }

    @Test
    void shouldReturnDistinctEffectivePermissions() {
        User user = createUser("effective-permissions");
        Role userRole = findRole(RoleName.USER);
        Role staffRole = findRole(RoleName.STAFF);

        userRoleRepository.saveAndFlush(new UserRole(
                user,
                userRole,
                ASSIGNED_AT,
                user));

        userRoleRepository.saveAndFlush(new UserRole(
                user,
                staffRole,
                ASSIGNED_AT,
                user));

        entityManager.clear();

        List<String> permissions = rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        user.getId());

        assertThat(permissions)
                .containsExactly(
                        "booking:cancel",
                        "booking:create",
                        "booking:read",
                        "inventory:manage",
                        "movie:manage",
                        "notification:manage",
                        "payment:read",
                        "showtime:manage");
    }

    @Test
    void shouldReturnEmptyEffectivePermissionsForUserWithoutRoles() {
        User user = createUser("without-roles");

        assertThat(rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        user.getId()))
                .isEmpty();
    }

    @Test
    void shouldRejectDuplicateRolePermission() {
        User assignedBy = createUser("duplicate-permission");
        Role userRole = findRole(RoleName.USER);
        Permission permission = findPermission(
                PermissionCode.BOOKING_READ);

        RolePermission duplicate = new RolePermission(
                userRole,
                permission,
                ASSIGNED_AT,
                assignedBy);

        assertThatThrownBy(() -> rolePermissionRepository
                .saveAndFlush(duplicate))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteRolePermission() {
        User assignedBy = createUser("delete-permission");
        Role serviceRole = findRole(RoleName.SERVICE);
        Permission permission = findPermission(
                PermissionCode.BOOKING_READ);

        rolePermissionRepository.saveAndFlush(
                new RolePermission(
                        serviceRole,
                        permission,
                        ASSIGNED_AT,
                        assignedBy));

        long deleted = rolePermissionRepository
                .deleteByRole_IdAndPermission_Id(
                        serviceRole.getId(),
                        permission.getId());

        rolePermissionRepository.flush();

        assertThat(deleted).isEqualTo(1L);

        assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_Id(
                        serviceRole.getId(),
                        permission.getId()))
                .isFalse();
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString();

        return userRepository.saveAndFlush(new User(
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix + "@example.com",
                prefix + "." + suffix,
                prefix + "." + suffix));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow();
    }

    private Permission findPermission(
            PermissionCode permissionCode) {

        return permissionRepository
                .findByCode(permissionCode.getCode())
                .orElseThrow();
    }
}
