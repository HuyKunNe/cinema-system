package com.cinema.inventory.repository;

import com.cinema.inventory.entity.RoomLayoutMatrixCell;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoomLayoutMatrixCellRepository extends JpaRepository<RoomLayoutMatrixCell, UUID> {

    @EntityGraph(attributePaths = "seat")
    List<RoomLayoutMatrixCell> findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(UUID layoutId);

    @Modifying(flushAutomatically = true)
    @Query(
            """
            delete from RoomLayoutMatrixCell cell
            where cell.layout.id = :layoutId
            """)
    void deleteAllForLayout(@Param("layoutId") UUID layoutId);

    @Query(
            """
            select cell
            from RoomLayoutMatrixCell cell
            join fetch cell.layout layout
            left join fetch cell.seat seat
            left join fetch seat.room
            where layout.id in :layoutIds
            order by layout.id, cell.rowIndex, cell.columnIndex
            """)
    List<RoomLayoutMatrixCell> findAllForCapacityByLayoutIds(
            @Param("layoutIds") Collection<UUID> layoutIds);
}
