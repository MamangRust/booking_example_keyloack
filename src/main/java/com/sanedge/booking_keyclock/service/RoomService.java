package com.sanedge.booking_keyclock.service;

import com.sanedge.booking_keyclock.domain.request.room.CreateRoomRequest;
import com.sanedge.booking_keyclock.domain.request.room.UpdateRoomRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;

public interface RoomService {
    MessageResponse findAll();

    MessageResponse findById(Long id);

    MessageResponse createRoom(CreateRoomRequest createRoomRequest);

    MessageResponse updateRoom(Long id, UpdateRoomRequest request);

    MessageResponse deleteRoom(Long id);
}
