package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserRoleAssignmentSecurityAuditTest {

    private static final UUID TARGET_USER_ID =
            UUID.fromString("019c5000-0000-7000-8000-000000000741");

    private static final UUID ACTOR_USER_ID =
            UUID.fromString("019c5000-0000-7000-8000-000000000742");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-17T12:00:00Z"), ZoneOffset.UTC);

    @Mock private UserRepository userRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private UserRoleRepository userRoleRepository;

    @Mock private SecurityAuditRecorder securityAuditRecorder;

    private UserRoleAssignmentServiceImpl service;

    private User targetUser;

    private User actorUser;

    private Role role;

    @BeforeEach
    void setUp() {
        service =
                new UserRoleAssignmentServiceImpl(
                        userRepository,
                        roleRepository,
                        userRoleRepository,
                        securityAuditRecorder,
                        FIXED_CLOCK);

        targetUser =
                new User("target@example.com", "target@example.com", "target-user", "target-user");

        actorUser =
                new User("admin@example.com", "admin@example.com", "audit-admin", "audit-admin");

        role = new Role(RoleName.ADMIN, "Administrator");

        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

        when(userRepository.findById(ACTOR_USER_ID)).thenReturn(Optional.of(actorUser));

        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(role));
    }

    @Test
    void assignRoleShouldRecordSecurityAudit() {
        when(userRoleRepository.existsByUser_IdAndRole_Id(targetUser.getId(), role.getId()))
                .thenReturn(false);

        service.assignRole(TARGET_USER_ID, RoleName.ADMIN, ACTOR_USER_ID);

        verify(userRoleRepository).saveAndFlush(any(UserRole.class));

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                SecurityAuditEventType.USER_ROLE_ASSIGNED,
                                SecurityAuditTargetType.USER,
                                targetUser.getId().toString(),
                                SecurityAuditOutcome.SUCCESS,
                                null,
                                "role=ADMIN"));
    }

    @Test
    void revokeRoleShouldRecordSecurityAudit() {
        when(userRoleRepository.deleteByUser_IdAndRole_Id(targetUser.getId(), role.getId()))
                .thenReturn(1L);

        service.revokeRole(TARGET_USER_ID, RoleName.ADMIN, ACTOR_USER_ID);

        verify(userRoleRepository).flush();

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                SecurityAuditEventType.USER_ROLE_REVOKED,
                                SecurityAuditTargetType.USER,
                                targetUser.getId().toString(),
                                SecurityAuditOutcome.SUCCESS,
                                null,
                                "role=ADMIN"));
    }

    @Test
    void duplicateAssignmentShouldNotRecordSecurityAudit() {
        when(userRoleRepository.existsByUser_IdAndRole_Id(targetUser.getId(), role.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assignRole(TARGET_USER_ID, RoleName.ADMIN, ACTOR_USER_ID))
                .isInstanceOf(ConflictException.class);

        verify(userRoleRepository, never()).saveAndFlush(any(UserRole.class));

        verify(securityAuditRecorder, never()).record(any(SecurityAuditRecord.class));
    }

    @Test
    void missingAssignmentShouldNotRecordSecurityAudit() {
        when(userRoleRepository.deleteByUser_IdAndRole_Id(targetUser.getId(), role.getId()))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.revokeRole(TARGET_USER_ID, RoleName.ADMIN, ACTOR_USER_ID))
                .isInstanceOf(ConflictException.class);

        verify(userRoleRepository, never()).flush();

        verify(securityAuditRecorder, never()).record(any(SecurityAuditRecord.class));
    }
}
