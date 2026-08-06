package com.cinema.user.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
import com.cinema.user.service.UserRoleAssignmentService;

@Service
@Validated
@Transactional(readOnly = true)
public class UserRoleAssignmentServiceImpl
        implements UserRoleAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserRoleAssignmentServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public void assignRole(
            UUID userId,
            RoleName roleName,
            UUID assignedByUserId) {

        if (roleName == RoleName.SERVICE) {
            throw new ConflictException(UserErrorCode.SERVICE_ROLE_NOT_ASSIGNABLE_TO_USER);
        }

        User user = findUser(userId);
        Role role = findRole(roleName);
        User assignedBy = findUser(assignedByUserId);

        if (userRoleRepository.existsByUser_IdAndRole_Id(
                user.getId(),
                role.getId())) {

            throw new ConflictException(UserErrorCode.USER_ROLE_ALREADY_ASSIGNED);
        }

        UserRole assignment = new UserRole(
                user,
                role,
                OffsetDateTime.now(ZoneOffset.UTC),
                assignedBy);

        try {
            userRoleRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(UserErrorCode.USER_ROLE_ALREADY_ASSIGNED, exception);
        }
    }

    @Override
    @Transactional
    public void revokeRole(
            UUID userId,
            RoleName roleName,
            UUID performedByUserId) {

        User user = findUser(userId);
        Role role = findRole(roleName);

        // Validate that the administrative actor exists.
        findUser(performedByUserId);

        long deleted = userRoleRepository
                .deleteByUser_IdAndRole_Id(
                        user.getId(),
                        role.getId());

        if (deleted == 0) {
            throw new ConflictException(UserErrorCode.USER_ROLE_NOT_ASSIGNED);
        }

        userRoleRepository.flush();
    }

    @Override
    public Set<RoleName> findRoles(UUID userId) {
        User user = findUser(userId);

        EnumSet<RoleName> roles = EnumSet.noneOf(RoleName.class);

        userRoleRepository
                .findAllByUser_Id(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .forEach(roles::add);

        return Set.copyOf(roles);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(
                        () -> new NotFoundException(UserErrorCode.ROLE_NOT_FOUND));
    }
}
