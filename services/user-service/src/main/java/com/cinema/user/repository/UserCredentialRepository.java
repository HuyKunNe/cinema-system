package com.cinema.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.user.entity.UserCredential;

public interface UserCredentialRepository
        extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);
}
