package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.Permission;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.entity.User;
import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.PermissionRepository;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.RolePermissionAssignmentService;
import com.cinema.user.service.UserRoleAssignmentService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Transactional
class RolePermissionSecurityAuditIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String ACTOR_USERNAME = "security-audit-admin";

    @Autowired private UserRoleAssignmentService userRoleAssignmentService;

    @Autowired private RolePermissionAssignmentService rolePermissionAssignmentService;

    @Autowired private UserRepository userRepository;

    @Autowired private RoleRepository roleRepository;

    @Autowired private PermissionRepository permissionRepository;

    @Autowired private RolePermissionRepository rolePermissionRepository;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

    @Autowired private EntityManager entityManager;

    @Autowired private Clock clock;

    @Test
    @WithMockUser(username = ACTOR_USERNAME, authorities = "user:manage")
    void userRoleChangesShouldPersistSecurityAuditEvents() {
        User actor = createActiveUser(ACTOR_USERNAME);

        User target = createActiveUser("role-target");

        userRoleAssignmentService.assignRole(target.getId(), RoleName.ADMIN, actor.getId());

        userRoleAssignmentService.revokeRole(target.getId(), RoleName.ADMIN, actor.getId());

        flushAndClear();

        List<SecurityAuditEvent> events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.USER, target.getId().toString());

        assertThat(events).hasSize(2);

        assertThat(events)
                .anySatisfy(
                        event ->
                                assertAuditEvent(
                                        event,
                                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                        SecurityAuditTargetType.USER,
                                        target.getId().toString(),
                                        "role=ADMIN"));

        assertThat(events)
                .anySatisfy(
                        event ->
                                assertAuditEvent(
                                        event,
                                        SecurityAuditEventType.USER_ROLE_REVOKED,
                                        SecurityAuditTargetType.USER,
                                        target.getId().toString(),
                                        "role=ADMIN"));
    }

    @Test
    @WithMockUser(username = ACTOR_USERNAME, authorities = "user:manage")
    void rolePermissionChangesShouldPersistSecurityAuditEvents() {
        User actor = createActiveUser(ACTOR_USERNAME);

        Role role = roleRepository.findByName(RoleName.STAFF).orElseThrow();

        Permission permission =
                permissionRepository
                        .findByCode(PermissionCode.MOVIE_MANAGE.getCode())
                        .orElseThrow();

        /*
         * Normalize the fixture because baseline migrations may already
         * assign this permission to the selected role.
         */
        rolePermissionRepository.deleteByRole_IdAndPermission_Id(role.getId(), permission.getId());

        rolePermissionRepository.flush();

        rolePermissionAssignmentService.assignPermission(
                RoleName.STAFF, PermissionCode.MOVIE_MANAGE, actor.getId());

        rolePermissionAssignmentService.revokePermission(
                RoleName.STAFF, PermissionCode.MOVIE_MANAGE, actor.getId());

        flushAndClear();

        List<SecurityAuditEvent> events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.ROLE, RoleName.STAFF.name());

        assertThat(events).hasSize(2);

        assertThat(events)
                .anySatisfy(
                        event ->
                                assertAuditEvent(
                                        event,
                                        SecurityAuditEventType.ROLE_PERMISSION_ASSIGNED,
                                        SecurityAuditTargetType.ROLE,
                                        RoleName.STAFF.name(),
                                        "permission=movie:manage"));

        assertThat(events)
                .anySatisfy(
                        event ->
                                assertAuditEvent(
                                        event,
                                        SecurityAuditEventType.ROLE_PERMISSION_REVOKED,
                                        SecurityAuditTargetType.ROLE,
                                        RoleName.STAFF.name(),
                                        "permission=movie:manage"));
    }

    private User createActiveUser(String prefix) {

        String suffix = UUID.randomUUID().toString();

        String username = prefix + "-" + suffix;

        String email = username + "@example.com";

        User user = new User(email, email.toLowerCase(), username, username.toLowerCase());

        user.verifyEmail(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        return userRepository.saveAndFlush(user);
    }

    private static void assertAuditEvent(
            SecurityAuditEvent event,
            SecurityAuditEventType eventType,
            SecurityAuditTargetType targetType,
            String targetReference,
            String metadata) {

        assertThat(event.getEventType()).isEqualTo(eventType);

        assertThat(event.getActorType()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(event.getActorReference()).isEqualTo(ACTOR_USERNAME);

        assertThat(event.getTargetType()).isEqualTo(targetType);

        assertThat(event.getTargetReference()).isEqualTo(targetReference);

        assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

        assertThat(event.getReason()).isNull();

        assertThat(event.getMetadata()).isEqualTo(metadata);

        assertThat(event.getCorrelationId()).isNull();

        assertThat(event.getOccurredAt()).isNotNull();

        assertThat(event.getCreatedAt()).isNotNull();

        assertThat(event.getUpdatedAt()).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
