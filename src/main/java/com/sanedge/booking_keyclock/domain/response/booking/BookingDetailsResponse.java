package com.sanedge.booking_keyclock.domain.response.booking;

import java.time.LocalDateTime;
import com.sanedge.booking_keyclock.enums.RoomStatus;

import lombok.Data;

@Data
public class BookingDetailsResponse {
    private String orderId;
    private Integer totalPerson;
    private LocalDateTime bookingTime;
    private String userId; 
    private String roomName;
    private Integer roomCapacity;
    private String roomPhoto;
    private RoomStatus roomStatus;

    public BookingDetailsResponse(String orderId, Integer totalPerson, LocalDateTime bookingTime,
            String userId, String roomName, Integer roomCapacity, // Disesuaikan juga di konstruktor
            String roomPhoto, RoomStatus roomStatus) {
        this.orderId = orderId;
        this.totalPerson = totalPerson;
        this.bookingTime = bookingTime;
        this.userId = userId; 
        this.roomName = roomName;
        this.roomCapacity = roomCapacity;
        this.roomPhoto = roomPhoto;
        this.roomStatus = roomStatus;
    }
}
