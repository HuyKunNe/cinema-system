package com.cinema.inventory.repository;

import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.enums.RoomLayoutStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomLayoutRepository extends JpaRepository<RoomLayout, UUID> {

    List<RoomLayout> findAllByRoom_IdOrderByLayoutVersionDesc(UUID roomId);

    Optional<RoomLayout> findByIdAndRoom_Id(UUID layoutId, UUID roomId);

    Optional<RoomLayout> findFirstByRoom_IdAndStatusOrderByLayoutVersionDesc(
            UUID roomId, RoomLayoutStatus status);

    boolean existsByRoom_IdAndLayoutVersion(UUID roomId, long layoutVersion);

    @Query(
            """
            select case when count(layout) > 0 then true else false end
            from RoomLayout layout
            where layout.room.id = :roomId
              and layout.status = :status
              and layout.rowCount is not null
              and layout.columnCount is not null
            """)
    boolean existsMatrixByRoomAndStatus(
            @Param("roomId") UUID roomId, @Param("status") RoomLayoutStatus status);

    @Query(
            """
            select coalesce(max(layout.layoutVersion), 0)
            from RoomLayout layout
            where layout.room.id = :roomId
            """)
    long findMaximumLayoutVersionByRoomId(@Param("roomId") UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select layout from RoomLayout layout where layout.id = :layoutId")
    Optional<RoomLayout> findByIdForUpdate(@Param("layoutId") UUID layoutId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select layout from RoomLayout layout where layout.id = :layoutId")
    Optional<RoomLayout> findByIdForRead(@Param("layoutId") UUID layoutId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            select layout
            from RoomLayout layout
            where layout.room.id = :roomId
              and layout.status = :status
            order by layout.layoutVersion desc
            """)
    List<RoomLayout> findPublishedCandidatesForRead(
            @Param("roomId") UUID roomId, @Param("status") RoomLayoutStatus status);

    @Query("select layout.room.id from RoomLayout layout where layout.id = :layoutId")
    Optional<UUID> findRoomIdByLayoutId(@Param("layoutId") UUID layoutId);
}
