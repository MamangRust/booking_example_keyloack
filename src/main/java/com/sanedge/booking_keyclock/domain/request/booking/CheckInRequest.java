package com.sanedge.booking_keyclock.domain.request.booking;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CheckInRequest {
    private String orderId;
    private LocalDateTime CheckInTime;
    private String email;
}
