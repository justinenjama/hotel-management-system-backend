package com.justine.repository;

import com.justine.model.Room;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumberAndHotelId(String roomNumber, Long hotelId);
}
