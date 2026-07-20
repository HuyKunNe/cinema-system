package com.cinema.common.core.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.common.core.uuid.UuidBinaryConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinaryConverter.class)
    protected UUID id;

    @Column(name = "created_at", nullable = false)
    protected OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    protected OffsetDateTime updatedAt;

}
