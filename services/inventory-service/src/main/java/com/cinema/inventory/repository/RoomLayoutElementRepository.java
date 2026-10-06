package com.cinema.inventory.repository;

import com.cinema.inventory.entity.RoomLayoutElement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomLayoutElementRepository extends JpaRepository<RoomLayoutElement, UUID> {

    List<RoomLayoutElement> findAllByLayout_IdOrderByIdAsc(UUID layoutId);
}
