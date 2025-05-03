package com.sanedge.booking_keyclock.service;

import com.sanedge.booking_keyclock.domain.request.booking.CreateBookingRequest;
import com.sanedge.booking_keyclock.domain.request.booking.UpdateBookingRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;

public interface BookingService {
    MessageResponse findAll();

    MessageResponse findById(Long id);

    MessageResponse createBooking(String userId, CreateBookingRequest request);

    MessageResponse updateBooking(Long id, String userId, UpdateBookingRequest request);

    MessageResponse deleteById(Long id);

}
