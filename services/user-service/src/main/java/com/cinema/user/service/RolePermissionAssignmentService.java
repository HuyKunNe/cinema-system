package com.cinema.user.service;

import java.util.Set;
import java.util.UUID;

import com.cinema.user.enums.PermissionCode;
import com.cinema.user.enums.RoleName;

import jakarta.validation.constraints.NotNull;

public interface RolePermissionAssignmentService {

    void assignPermission(
            @NotNull RoleName roleName,
            @NotNull PermissionCode permissionCode,
            @NotNull UUID assignedByUserId);

    void revokePermission(
            @NotNull RoleName roleName,
            @NotNull PermissionCode permissionCode,
            @NotNull UUID performedByUserId);

    Set<String> findPermissions(
            @NotNull RoleName roleName);

    Set<String> findEffectivePermissions(
            @NotNull UUID userId);
}
