package com.cinema.user.repository;

import com.cinema.user.entity.UserExternalIdentity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserExternalIdentityRepository extends JpaRepository<UserExternalIdentity, UUID> {

    Optional<UserExternalIdentity> findByIssuerAndSubject(String issuer, String subject);
}
