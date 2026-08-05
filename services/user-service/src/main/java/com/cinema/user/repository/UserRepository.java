package com.cinema.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByNormalizedEmail(String normalizedEmail);

    Optional<User> findByNormalizedUsername(String normalizedUsername);

    Optional<User> findByNormalizedEmailOrNormalizedUsername(
            String normalizedEmail,
            String normalizedUsername);

    boolean existsByNormalizedEmail(String normalizedEmail);

    boolean existsByNormalizedUsername(String normalizedUsername);
}
