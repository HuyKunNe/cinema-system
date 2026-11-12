package com.cinema.user.entity;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_external_identities",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_external_identity_issuer_subject",
                    columnNames = {"issuer", "subject"}),
            @UniqueConstraint(
                    name = "uk_user_external_identity_user_issuer",
                    columnNames = {"user_id", "issuer"})
        })
public class UserExternalIdentity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_external_identity_user"))
    private User user;

    @Column(
            name = "issuer",
            nullable = false,
            length = 255,
            columnDefinition = "VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin")
    private String issuer;

    @Column(
            name = "subject",
            nullable = false,
            length = 255,
            columnDefinition = "VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin")
    private String subject;

    protected UserExternalIdentity() {}

    public UserExternalIdentity(User user, String issuer, String subject) {
        this.user = user;
        this.issuer = issuer;
        this.subject = subject;
    }

    public User getUser() {
        return user;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }
}
