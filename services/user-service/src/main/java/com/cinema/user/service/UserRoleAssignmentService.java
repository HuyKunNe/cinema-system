package com.cinema.user.service;

import java.util.Set;
import java.util.UUID;

import com.cinema.user.enums.RoleName;

import jakarta.validation.constraints.NotNull;

public interface UserRoleAssignmentService {

    void assignRole(
            @NotNull UUID userId,
            @NotNull RoleName roleName,
            @NotNull UUID assignedByUserId);

    void revokeRole(
            @NotNull UUID userId,
            @NotNull RoleName roleName,
            @NotNull UUID performedByUserId);

    Set<RoleName> findRoles(
            @NotNull UUID userId);
}
