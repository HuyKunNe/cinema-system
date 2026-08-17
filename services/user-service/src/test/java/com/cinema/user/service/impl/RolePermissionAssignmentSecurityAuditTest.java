package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.user.entity.Permission;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.RolePermission;
import com.cinema.user.entity.User;
import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.PermissionRepository;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
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
class RolePermissionAssignmentSecurityAuditTest {

    private static final UUID ACTOR_USER_ID =
            UUID.fromString("019c5000-0000-7000-8000-000000000743");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-17T12:30:00Z"), ZoneOffset.UTC);

    @Mock private UserRepository userRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private PermissionRepository permissionRepository;

    @Mock private RolePermissionRepository rolePermissionRepository;

    @Mock private SecurityAuditRecorder securityAuditRecorder;

    private RolePermissionAssignmentServiceImpl service;

    private User actorUser;

    private Role role;

    private Permission permission;

    @BeforeEach
    void setUp() {
        service =
                new RolePermissionAssignmentServiceImpl(
                        userRepository,
                        roleRepository,
                        permissionRepository,
                        rolePermissionRepository,
                        securityAuditRecorder,
                        FIXED_CLOCK);

        actorUser =
                new User("admin@example.com", "admin@example.com", "audit-admin", "audit-admin");

        role = new Role(RoleName.STAFF, "Staff");

        permission = new Permission(PermissionCode.MOVIE_MANAGE, "Manage movies");

        when(userRepository.findById(ACTOR_USER_ID)).thenReturn(Optional.of(actorUser));

        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.of(role));

        when(permissionRepository.findByCode(PermissionCode.MOVIE_MANAGE.getCode()))
                .thenReturn(Optional.of(permission));
    }

    @Test
    void assignPermissionShouldRecordSecurityAudit() {
        when(rolePermissionRepository.existsByRole_IdAndPermission_Id(
                        role.getId(), permission.getId()))
                .thenReturn(false);

        service.assignPermission(RoleName.STAFF, PermissionCode.MOVIE_MANAGE, ACTOR_USER_ID);

        verify(rolePermissionRepository).saveAndFlush(any(RolePermission.class));

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                SecurityAuditEventType.ROLE_PERMISSION_ASSIGNED,
                                SecurityAuditTargetType.ROLE,
                                "STAFF",
                                SecurityAuditOutcome.SUCCESS,
                                null,
                                "permission=movie:manage"));
    }

    @Test
    void revokePermissionShouldRecordSecurityAudit() {
        when(rolePermissionRepository.deleteByRole_IdAndPermission_Id(
                        role.getId(), permission.getId()))
                .thenReturn(1L);

        service.revokePermission(RoleName.STAFF, PermissionCode.MOVIE_MANAGE, ACTOR_USER_ID);

        verify(rolePermissionRepository).flush();

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                SecurityAuditEventType.ROLE_PERMISSION_REVOKED,
                                SecurityAuditTargetType.ROLE,
                                "STAFF",
                                SecurityAuditOutcome.SUCCESS,
                                null,
                                "permission=movie:manage"));
    }

    @Test
    void duplicateAssignmentShouldNotRecordSecurityAudit() {
        when(rolePermissionRepository.existsByRole_IdAndPermission_Id(
                        role.getId(), permission.getId()))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.assignPermission(
                                        RoleName.STAFF, PermissionCode.MOVIE_MANAGE, ACTOR_USER_ID))
                .isInstanceOf(ConflictException.class);

        verify(rolePermissionRepository, never()).saveAndFlush(any(RolePermission.class));

        verify(securityAuditRecorder, never()).record(any(SecurityAuditRecord.class));
    }

    @Test
    void missingAssignmentShouldNotRecordSecurityAudit() {
        when(rolePermissionRepository.deleteByRole_IdAndPermission_Id(
                        role.getId(), permission.getId()))
                .thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                service.revokePermission(
                                        RoleName.STAFF, PermissionCode.MOVIE_MANAGE, ACTOR_USER_ID))
                .isInstanceOf(ConflictException.class);

        verify(rolePermissionRepository, never()).flush();

        verify(securityAuditRecorder, never()).record(any(SecurityAuditRecord.class));
    }
}
