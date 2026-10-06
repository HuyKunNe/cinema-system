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
}
