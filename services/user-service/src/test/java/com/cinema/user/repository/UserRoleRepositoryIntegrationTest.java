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
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;

import jakarta.persistence.EntityManager;

@Transactional
class UserRoleRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime ASSIGNED_AT = OffsetDateTime.parse("2026-08-01T03:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistUserRoleWithCompositeId() {
        User user = createUser("persist");
        Role role = findRole(RoleName.USER);

        UserRole assignment = new UserRole(
                user,
                role,
                ASSIGNED_AT,
                user);

        UserRole saved = userRoleRepository
                .saveAndFlush(assignment);

        assertThat(saved.getId().getUserId())
                .isEqualTo(user.getId());

        assertThat(saved.getId().getRoleId())
                .isEqualTo(role.getId());

        assertThat(saved.getAssignedAt())
                .isEqualTo(ASSIGNED_AT);

        assertThat(saved.getAssignedBy().getId())
                .isEqualTo(user.getId());

        assertThat(saved.isNew()).isFalse();
    }

    @Test
    void shouldFindRolesByUserId() {
        User user = createUser("find-roles");
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

        List<UserRole> assignments = userRoleRepository
                .findAllByUser_Id(user.getId());

        assertThat(assignments)
                .extracting(assignment -> assignment.getRole().getName())
                .containsExactlyInAnyOrder(
                        RoleName.USER,
                        RoleName.STAFF);
    }

    @Test
    void shouldRejectDuplicateUserRole() {
        User user = createUser("duplicate");
        Role role = findRole(RoleName.USER);

        userRoleRepository.saveAndFlush(new UserRole(
                user,
                role,
                ASSIGNED_AT,
                user));

        entityManager.clear();

        UserRole duplicate = new UserRole(
                user,
                role,
                ASSIGNED_AT.plusMinutes(1),
                user);

        assertThatThrownBy(() -> userRoleRepository.saveAndFlush(duplicate))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void shouldDetectExistingUserRole() {
        User user = createUser("exists");
        Role role = findRole(RoleName.USER);

        userRoleRepository.saveAndFlush(new UserRole(
                user,
                role,
                ASSIGNED_AT,
                user));

        assertThat(userRoleRepository
                .existsByUser_IdAndRole_Id(
                        user.getId(),
                        role.getId()))
                .isTrue();
    }

    @Test
    void shouldDeleteUserRole() {
        User user = createUser("delete");
        Role role = findRole(RoleName.USER);

        userRoleRepository.saveAndFlush(new UserRole(
                user,
                role,
                ASSIGNED_AT,
                user));

        long deleted = userRoleRepository
                .deleteByUser_IdAndRole_Id(
                        user.getId(),
                        role.getId());

        userRoleRepository.flush();

        assertThat(deleted).isEqualTo(1L);

        assertThat(userRoleRepository
                .existsByUser_IdAndRole_Id(
                        user.getId(),
                        role.getId()))
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
}
