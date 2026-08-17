package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.UserRoleAssignmentService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class UserRoleAssignmentServiceImpl implements UserRoleAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SecurityAuditRecorder securityAuditRecorder;
    private final Clock clock;

    public UserRoleAssignmentServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SecurityAuditRecorder securityAuditRecorder,
            Clock clock) {

        this.userRepository = userRepository;

        this.roleRepository = roleRepository;

        this.userRoleRepository = userRoleRepository;

        this.securityAuditRecorder = securityAuditRecorder;

        this.clock = clock;
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, RoleName roleName, UUID assignedByUserId) {

        if (roleName == RoleName.SERVICE) {
            throw new ConflictException(UserErrorCode.SERVICE_ROLE_NOT_ASSIGNABLE_TO_USER);
        }

        User user = findUser(userId);
        Role role = findRole(roleName);
        User assignedBy = findUser(assignedByUserId);

        if (userRoleRepository.existsByUser_IdAndRole_Id(user.getId(), role.getId())) {

            throw new ConflictException(UserErrorCode.USER_ROLE_ALREADY_ASSIGNED);
        }

        UserRole assignment = new UserRole(user, role, OffsetDateTime.now(clock), assignedBy);

        try {
            userRoleRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(UserErrorCode.USER_ROLE_ALREADY_ASSIGNED, exception);
        }
        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.USER_ROLE_ASSIGNED,
                        SecurityAuditTargetType.USER,
                        user.getId().toString(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        roleMetadata(role.getName())));
    }

    @Override
    @Transactional
    public void revokeRole(UUID userId, RoleName roleName, UUID performedByUserId) {

        User user = findUser(userId);
        Role role = findRole(roleName);

        // Validate that the administrative actor exists.
        findUser(performedByUserId);

        long deleted = userRoleRepository.deleteByUser_IdAndRole_Id(user.getId(), role.getId());

        if (deleted == 0) {
            throw new ConflictException(UserErrorCode.USER_ROLE_NOT_ASSIGNED);
        }

        userRoleRepository.flush();
        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.USER_ROLE_REVOKED,
                        SecurityAuditTargetType.USER,
                        user.getId().toString(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        roleMetadata(role.getName())));
    }

    @Override
    public Set<RoleName> findRoles(UUID userId) {
        User user = findUser(userId);

        EnumSet<RoleName> roles = EnumSet.noneOf(RoleName.class);

        userRoleRepository.findAllByUser_Id(user.getId()).stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .forEach(roles::add);

        return Set.copyOf(roles);
    }

    private static String roleMetadata(RoleName roleName) {

        return "role=" + roleName.name();
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.ROLE_NOT_FOUND));
    }
}
