package com.sanedge.booking_keyclock.service;

import com.sanedge.booking_keyclock.domain.request.booking.CheckInRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;

public interface CheckService {
    MessageResponse checkInOrder(CheckInRequest request);

    MessageResponse checkOrder(String orderId);

    MessageResponse checkOutOrder(String orderId);
}
