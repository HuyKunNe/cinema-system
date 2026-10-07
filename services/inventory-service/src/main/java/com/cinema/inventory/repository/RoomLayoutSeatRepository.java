package com.cinema.inventory.repository;

import com.cinema.inventory.entity.RoomLayoutSeat;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoomLayoutSeatRepository extends JpaRepository<RoomLayoutSeat, UUID> {

    @EntityGraph(attributePaths = {"seat"})
    List<RoomLayoutSeat> findAllByLayout_IdOrderBySeatNumberSnapshotAsc(UUID layoutId);

    @Modifying(flushAutomatically = true)
    @Query(
            """
            delete from RoomLayoutSeat layoutSeat
            where layoutSeat.layout.id = :layoutId
            """)
    void deleteAllForLayout(@Param("layoutId") UUID layoutId);

    @Query(
            """
            select position
            from RoomLayoutSeat position
            where position.layout.id = :layoutId
            order by position.seatNumberSnapshot asc
            """)
    List<RoomLayoutSeat> findPositionsForGenerationByLayoutId(@Param("layoutId") UUID layoutId);

    @Query(
            """
            select position
            from RoomLayoutSeat position
            join fetch position.seat seat
            join fetch seat.room
            where position.layout.id = :layoutId
            order by position.seatNumberSnapshot asc
            """)
    List<RoomLayoutSeat> findAllForSeatMapByLayoutId(@Param("layoutId") UUID layoutId);
}
