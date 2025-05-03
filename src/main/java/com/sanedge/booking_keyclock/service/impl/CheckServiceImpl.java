package com.sanedge.booking_keyclock.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.booking_keyclock.domain.request.booking.CheckInRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.domain.response.booking.BookingDetailsResponse;
import com.sanedge.booking_keyclock.domain.response.booking.CheckOutResponse;
import com.sanedge.booking_keyclock.enums.RoomStatus;
import com.sanedge.booking_keyclock.exception.ResourceNotFoundException;
import com.sanedge.booking_keyclock.models.Booking;
import com.sanedge.booking_keyclock.models.Room;
import com.sanedge.booking_keyclock.repository.BookingRepository;
import com.sanedge.booking_keyclock.repository.RoomRepository;
import com.sanedge.booking_keyclock.service.BookingMailService;
import com.sanedge.booking_keyclock.service.CheckService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class CheckServiceImpl implements CheckService {
    @Value("${keycloak.realm}")
    private String keycloakRealm;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMailService bookingMailService;
    private final Keycloak keycloak;

    @Autowired
    public CheckServiceImpl(BookingRepository bookingRepository,
            RoomRepository roomRepository,
            BookingMailService bookingMailService,
            Keycloak keycloak) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingMailService = bookingMailService;
        this.keycloak = keycloak;
    }

    @Override
    public MessageResponse checkInOrder(CheckInRequest request) {
        try {
            log.info("Checking in order with orderId: {}", request.getOrderId());

            Booking booking = bookingRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            UserRepresentation user = getUserFromKeycloak(booking.getUserId());

            booking.setCheckInTime(request.getCheckInTime());
            booking.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
            bookingRepository.save(booking);

            bookingMailService.sendEmailCheckIn(
                    booking.getOrderId(),
                    user.getEmail(),
                    booking.getCheckInTime().toString());

            log.info("Order checked in successfully: {}", booking.getOrderId());
            return MessageResponse.builder()
                    .message("Check-in successful")
                    .statusCode(HttpStatus.OK.value())
                    .build();

        } catch (ResourceNotFoundException e) {
            log.warn("Check-in failed: {}", e.getMessage());
            return MessageResponse.builder()
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build();
        } catch (Exception e) {
            log.error("Error checking in order: {}", e.getMessage(), e);
            return MessageResponse.builder()
                    .message("Failed to process check-in")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    public BookingDetailsResponse getBookingDetails(String orderId) {
        BookingDetailsResponse bookingDetails = bookingRepository.findBookingDetailsByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        UserRepresentation user = keycloak.realm(this.keycloakRealm)
                .users()
                .get(bookingDetails.getUserId())
                .toRepresentation();

        bookingDetails.setUserId(user.getId());

        return bookingDetails;
    }

    @Override
    public MessageResponse checkOrder(String orderId) {
        try {
            log.info("Checking order details for orderId: {}", orderId);

            BookingDetailsResponse bookingDetails = bookingRepository.findBookingDetailsByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            UserRepresentation user = getUserEmailFromKeycloak(bookingDetails.getUserId());
            bookingDetails.setUserId(user.getId());

            log.info("Found order details: {}", orderId);
            return MessageResponse.builder()
                    .message("Booking details retrieved")
                    .data(bookingDetails)
                    .statusCode(HttpStatus.OK.value())
                    .build();

        } catch (ResourceNotFoundException e) {
            log.warn("Check order failed: {}", e.getMessage());
            return MessageResponse.builder()
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build();
        } catch (Exception e) {
            log.error("Error checking order details: {}", e.getMessage(), e);
            return MessageResponse.builder()
                    .message("Failed to retrieve booking details")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    @Override
    public MessageResponse checkOutOrder(String orderId) {
        try {
            log.info("Processing check-out for order: {}", orderId);

            Booking booking = bookingRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            if (booking.getCheckOutTime() != null) {
                return MessageResponse.builder()
                        .message("Already checked out")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
            }

            Room room = roomRepository.findById(booking.getRoom().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

            UserRepresentation user = getUserFromKeycloak(booking.getUserId());

            LocalDateTime checkOutTime = LocalDateTime.now();
            booking.setCheckOutTime(checkOutTime);
            booking.setUpdatedAt(Timestamp.valueOf(checkOutTime));
            room.setRoomStatus(RoomStatus.READY);
            room.setUpdatedAt(Timestamp.valueOf(checkOutTime));

            bookingRepository.save(booking);
            roomRepository.save(room);

            bookingMailService.sendEmailCheckOut(
                    booking.getOrderId(),
                    user.getEmail(),
                    checkOutTime.toString());

            CheckOutResponse response = new CheckOutResponse(
                    booking.getOrderId(),
                    booking.getRoom().getId(),
                    booking.getCheckInTime(), 
                    checkOutTime,
                    user.getEmail()
            );

            log.info("Check-out processed successfully: {}", orderId);
            return MessageResponse.builder()
                    .message("Check-out successful")
                    .data(response)
                    .statusCode(HttpStatus.OK.value())
                    .build();

        } catch (ResourceNotFoundException e) {
            log.warn("Check-out failed: {}", e.getMessage());
            return MessageResponse.builder()
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build();
        } catch (Exception e) {
            log.error("Error processing check-out: {}", e.getMessage(), e);
            return MessageResponse.builder()
                    .message("Failed to process check-out")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    private UserRepresentation getUserFromKeycloak(String userId) {
        try {
            UserResource userResource = keycloak.realm(this.keycloakRealm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null || !user.isEnabled()) {
                throw new ResourceNotFoundException("User not found or account disabled");
            }
            return user;
        } catch (Exception e) {
            log.error("Error fetching user from Keycloak: {}", e.getMessage());
            throw new ResourceNotFoundException("Failed to retrieve user information");
        }
    }

    private UserRepresentation getUserEmailFromKeycloak(String email) {
        try {
            UserResource userResource = keycloak.realm(this.keycloakRealm).users().get(email);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null || !user.isEnabled()) {
                throw new ResourceNotFoundException("User not found or account disabled");
            }
            return user;
        } catch (Exception e) {
            log.error("Error fetching user from Keycloak: {}", e.getMessage());
            throw new ResourceNotFoundException("Failed to retrieve user information");
        }
    }
}