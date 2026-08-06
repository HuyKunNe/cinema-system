package com.cinema.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByCodeIn(Collection<String> codes);

    List<Permission> findAllByOrderByCodeAsc();
}
