package com.cinema.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.RoomLayoutElement;

public interface RoomLayoutElementRepository
        extends JpaRepository<RoomLayoutElement, UUID> {

    List<RoomLayoutElement> findAllByLayout_IdOrderByIdAsc(
            UUID layoutId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from RoomLayoutElement layoutElement
            where layoutElement.layout.id = :layoutId
            """)
    void deleteAllForLayout(
            @Param("layoutId") UUID layoutId);
}
