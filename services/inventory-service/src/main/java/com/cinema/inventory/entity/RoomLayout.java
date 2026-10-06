package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_layouts")
public class RoomLayout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @Column(name = "layout_version", nullable = false, updatable = false)
    private long layoutVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RoomLayoutStatus status = RoomLayoutStatus.DRAFT;

    @Column(name = "canvas_width", nullable = false, precision = 12, scale = 3)
    private BigDecimal canvasWidth;

    @Column(name = "canvas_height", nullable = false, precision = 12, scale = 3)
    private BigDecimal canvasHeight;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "content_revision", nullable = false)
    private long contentRevision = 0L;

    protected RoomLayout() {}

    public RoomLayout(
            Room room, long layoutVersion, BigDecimal canvasWidth, BigDecimal canvasHeight) {

        if (room == null || layoutVersion <= 0) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        this.room = room;
        this.layoutVersion = layoutVersion;
        this.canvasWidth = RoomLayoutGeometry.requirePositiveDimension(canvasWidth);
        this.canvasHeight = RoomLayoutGeometry.requirePositiveDimension(canvasHeight);
        this.status = RoomLayoutStatus.DRAFT;
    }

    public Room getRoom() {
        return room;
    }

    public long getLayoutVersion() {
        return layoutVersion;
    }

    public RoomLayoutStatus getStatus() {
        return status;
    }

    public BigDecimal getCanvasWidth() {
        return canvasWidth;
    }

    public BigDecimal getCanvasHeight() {
        return canvasHeight;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void requireDraft() {
        if (status != RoomLayoutStatus.DRAFT) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_NOT_DRAFT);
        }
    }

    public void markContentChanged() {
        requireDraft();
        contentRevision = Math.addExact(contentRevision, 1L);
    }
}
