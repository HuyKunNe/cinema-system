package com.cinema.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.user.entity.RolePermission;
import com.cinema.user.entity.id.RolePermissionId;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findAllByRole_Id(UUID roleId);

    List<RolePermission> findAllByPermission_Id(UUID permissionId);

    boolean existsByRole_IdAndPermission_Id(
            UUID roleId,
            UUID permissionId);

    long deleteByRole_IdAndPermission_Id(
            UUID roleId,
            UUID permissionId);

    @Query("""
            select distinct rp.permission.code
            from RolePermission rp
            where rp.role.id in (
                select ur.role.id
                from UserRole ur
                where ur.user.id = :userId
            )
            order by rp.permission.code
            """)
    List<String> findEffectivePermissionCodesByUserId(
            @Param("userId") UUID userId);
}
