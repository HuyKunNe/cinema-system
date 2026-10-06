package com.cinema.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.RoomLayout;

public interface RoomLayoutRepository
        extends JpaRepository<RoomLayout, UUID> {

    List<RoomLayout> findAllByRoom_IdOrderByLayoutVersionDesc(
            UUID roomId);

    Optional<RoomLayout> findByIdAndRoom_Id(
            UUID layoutId,
            UUID roomId);

    boolean existsByRoom_IdAndLayoutVersion(
            UUID roomId,
            long layoutVersion);

    @Query("""
            select coalesce(max(layout.layoutVersion), 0)
            from RoomLayout layout
            where layout.room.id = :roomId
            """)
    long findMaximumLayoutVersionByRoomId(
            @Param("roomId") UUID roomId);
}
