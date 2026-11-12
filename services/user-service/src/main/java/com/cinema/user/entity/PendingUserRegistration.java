package com.cinema.user.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pending_user_registrations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pending_registration_normalized_email",
                    columnNames = "normalized_email"),
            @UniqueConstraint(
                    name = "uk_pending_registration_token_hash",
                    columnNames = "token_hash")
        })
public class PendingUserRegistration extends BaseEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(
            name = "token_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected PendingUserRegistration() {}

    public PendingUserRegistration(
            String email,
            String normalizedEmail,
            String firstName,
            String lastName,
            String phoneNumber,
            String tokenHash,
            OffsetDateTime expiresAt) {
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void replace(
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            String tokenHash,
            OffsetDateTime expiresAt) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(OffsetDateTime now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
