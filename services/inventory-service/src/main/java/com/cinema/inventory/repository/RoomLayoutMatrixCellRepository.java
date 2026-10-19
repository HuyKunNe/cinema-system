package com.cinema.inventory.repository;

import com.cinema.inventory.entity.RoomLayoutMatrixCell;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
