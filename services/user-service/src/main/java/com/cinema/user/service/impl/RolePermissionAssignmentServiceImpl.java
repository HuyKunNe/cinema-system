package com.cinema.user.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.user.entity.Permission;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.RolePermission;
import com.cinema.user.entity.User;
import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.PermissionRepository;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.RolePermissionAssignmentService;

@Service
@Validated
@Transactional(readOnly = true)
public class RolePermissionAssignmentServiceImpl
        implements RolePermissionAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final Clock clock;

    public RolePermissionAssignmentServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            Clock clock) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void assignPermission(
            RoleName roleName,
            PermissionCode permissionCode,
            UUID assignedByUserId) {

        if (roleName == RoleName.SERVICE) {
            throw new ConflictException(UserErrorCode.SERVICE_ROLE_PERMISSION_NOT_ALLOWED);
        }

        Role role = findRole(roleName);
        Permission permission = findPermission(permissionCode);

        User assignedBy = findUser(assignedByUserId);

        if (rolePermissionRepository
                .existsByRole_IdAndPermission_Id(
                        role.getId(),
                        permission.getId())) {

            throw new ConflictException(UserErrorCode.ROLE_PERMISSION_ALREADY_ASSIGNED);
        }

        RolePermission assignment = new RolePermission(
                role,
                permission,
                OffsetDateTime.now(clock),
                assignedBy);

        try {
            rolePermissionRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(UserErrorCode.ROLE_PERMISSION_ALREADY_ASSIGNED, exception);
        }
    }

    @Override
    @Transactional
    public void revokePermission(
            RoleName roleName,
            PermissionCode permissionCode,
            UUID performedByUserId) {

        Role role = findRole(roleName);
        Permission permission = findPermission(permissionCode);

        // Validate that the administrative actor exists.
        findUser(performedByUserId);

        long deleted = rolePermissionRepository
                .deleteByRole_IdAndPermission_Id(
                        role.getId(),
                        permission.getId());

        if (deleted == 0) {
            throw new ConflictException(UserErrorCode.ROLE_PERMISSION_NOT_ASSIGNED);
        }

        rolePermissionRepository.flush();
    }

    @Override
    public Set<String> findPermissions(RoleName roleName) {
        Role role = findRole(roleName);

        TreeSet<String> permissions = new TreeSet<>();

        rolePermissionRepository
                .findAllByRole_Id(role.getId())
                .stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .forEach(permissions::add);

        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public Set<String> findEffectivePermissions(UUID userId) {
        User user = findUser(userId);

        TreeSet<String> permissions = new TreeSet<>(
                rolePermissionRepository
                        .findEffectivePermissionCodesByUserId(
                                user.getId()));

        return Collections.unmodifiableSet(permissions);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(
                        () -> new NotFoundException(UserErrorCode.ROLE_NOT_FOUND));
    }

    private Permission findPermission(
            PermissionCode permissionCode) {

        return permissionRepository
                .findByCode(permissionCode.getCode())
                .orElseThrow(
                        () -> new NotFoundException(UserErrorCode.PERMISSION_NOT_FOUND));
    }
}
