package com.cinema.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.UserRole;
import com.cinema.user.entity.id.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findAllByUser_Id(UUID userId);

    List<UserRole> findAllByRole_Id(UUID roleId);

    boolean existsByUser_IdAndRole_Id(
            UUID userId,
            UUID roleId);

    long deleteByUser_IdAndRole_Id(
            UUID userId,
            UUID roleId);
}
