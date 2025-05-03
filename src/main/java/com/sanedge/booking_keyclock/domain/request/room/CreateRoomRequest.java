package com.sanedge.booking_keyclock.domain.request.room;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String roomName;
    private Integer roomCapacity;
    private MultipartFile file;
}
