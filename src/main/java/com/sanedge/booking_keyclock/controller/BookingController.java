package com.sanedge.booking_keyclock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanedge.booking_keyclock.domain.request.booking.CreateBookingRequest;
import com.sanedge.booking_keyclock.domain.request.booking.UpdateBookingRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.service.AuthService;
import com.sanedge.booking_keyclock.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    BookingService bookingService;

    @Autowired
    AuthService authService;

    @GetMapping("")
    public ResponseEntity<?> findAll() {
        MessageResponse room = bookingService.findAll();

        return ResponseEntity.ok(room);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> findById(@RequestParam("id") Long id) {
        MessageResponse room = bookingService.findById(id);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/create")
    public ResponseEntity<MessageResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {

        MessageResponse room = bookingService.createBooking(authService.getCurrentUser().getId(), request);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<MessageResponse> updateBooking(@RequestParam("id") Long id,
            @Valid @RequestBody UpdateBookingRequest request) {

        MessageResponse room = bookingService.updateBooking(id, authService.getCurrentUser().getId(), request);

        return ResponseEntity.ok(room);
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<MessageResponse> deleteBooking(@RequestParam("id") Long id) {

        MessageResponse room = bookingService.deleteById(id);

        return ResponseEntity.ok(room);
    }

}
