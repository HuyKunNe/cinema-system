package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_layout_elements")
public class RoomLayoutElement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false, updatable = false)
    private RoomLayout layout;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private RoomLayoutElementKind kind;

    @Column(name = "label", length = 150)
    private String label;

    @Embedded private RoomLayoutGeometry geometry;

    protected RoomLayoutElement() {}

    public RoomLayoutElement(
            RoomLayout layout,
            RoomLayoutElementKind kind,
            String label,
            RoomLayoutGeometry geometry) {

        if (layout == null
                || kind == null
                || geometry == null
                || (label != null && label.length() > 150)) {

            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        layout.requireDraft();

        this.layout = layout;
        this.kind = kind;
        this.label = label;
        this.geometry = geometry;
    }

    public RoomLayout getLayout() {
        return layout;
    }

    public RoomLayoutElementKind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }

    public RoomLayoutGeometry getGeometry() {
        return geometry;
    }
}
