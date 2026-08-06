package com.cinema.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.Role;
import com.cinema.user.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);

    List<Role> findAllByOrderByNameAsc();
}
