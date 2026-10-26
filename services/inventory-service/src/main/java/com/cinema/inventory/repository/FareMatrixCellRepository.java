package com.cinema.inventory.repository;

import com.cinema.inventory.entity.FareMatrixCell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FareMatrixCellRepository extends JpaRepository<FareMatrixCell, UUID> {

    List<FareMatrixCell> findAllByPolicyVersion_Id(UUID versionId);

    @Modifying
    @Query(
            """
            delete from FareMatrixCell cell
            where cell.policyVersion.id = :versionId
            """)
    int deleteForVersion(@Param("versionId") UUID versionId);
}
