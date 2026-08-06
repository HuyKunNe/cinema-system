package com.cinema.user.entity;

import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.user.enums.RoleName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
})
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private RoleName name;

    @Column(name = "description", length = 255)
    private String description;

    protected Role() {
    }

    public Role(
            RoleName name,
            String description) {

        this.name = name;
        this.description = description;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public RoleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
