package com.sanedge.booking_keyclock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.booking_keyclock.models.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAll();

    Optional<Room> findByRoomName(String roomName);

    Optional<Room> findById(Long id);

}
