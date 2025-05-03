package com.sanedge.booking_keyclock.domain.response.booking;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CheckOutResponse {
    private String orderId;
    private Long roomId;
    private LocalDateTime checkInTime;  
    private LocalDateTime checkOutTime;
    private String userEmail;  

    public CheckOutResponse(String orderId, Long roomId, LocalDateTime checkOutTime, String userId) {
        this.orderId = orderId;
        this.roomId = roomId;
        this.checkOutTime = checkOutTime;
        this.userEmail = userId;
    }

    public CheckOutResponse(String orderId, Long roomId, LocalDateTime checkInTime, LocalDateTime checkOutTime, String userId) {
        this.orderId = orderId;
        this.roomId = roomId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.userEmail = userId;
    }
}
