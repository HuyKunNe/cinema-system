package com.cinema.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.inventory.entity.RoomLayoutSeat;

public interface RoomLayoutSeatRepository
        extends JpaRepository<RoomLayoutSeat, UUID> {

    List<RoomLayoutSeat>
            findAllByLayout_IdOrderBySeatNumberSnapshotAsc(
                    UUID layoutId);
}
