package com.sanedge.booking_keyclock.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.booking_keyclock.config.KeycloakConfig;
import com.sanedge.booking_keyclock.domain.request.booking.CheckInRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.domain.response.booking.BookingDetailsResponse;
import com.sanedge.booking_keyclock.domain.response.booking.CheckOutResponse;
import com.sanedge.booking_keyclock.enums.RoomStatus;
import com.sanedge.booking_keyclock.exception.ResourceNotFoundException;
import com.sanedge.booking_keyclock.models.Booking;
import com.sanedge.booking_keyclock.models.Room;
import com.sanedge.booking_keyclock.models.User;
import com.sanedge.booking_keyclock.repository.BookingRepository;
import com.sanedge.booking_keyclock.repository.RoomRepository;
import com.sanedge.booking_keyclock.service.BookingMailService;
import com.sanedge.booking_keyclock.service.CheckService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class CheckServiceImpl implements CheckService {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMailService bookingMailService;

    private final KeycloakConfig keycloakConfig;

    @Autowired
    public CheckServiceImpl(BookingRepository bookingRepository,
            RoomRepository roomRepository,
            BookingMailService bookingMailService,
            KeycloakConfig keycloakConfig) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingMailService = bookingMailService;
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public MessageResponse checkInOrder(CheckInRequest request) {
        try {
            log.info("Checking in order with orderId: {}", request.getOrderId());

            Booking booking = bookingRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            Optional<User> user = getUserByEmail(request.getEmail());

            if (user.isEmpty()) {
                throw new ResourceNotFoundException("User not found");
            }

            booking.setCheckInTime(request.getCheckInTime());
            booking.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
            bookingRepository.save(booking);

            bookingMailService.sendEmailCheckIn(
                    booking.getOrderId(),
                    user.get().getEmail(),
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

        RealmResource realmResource = keycloakConfig.getRealmResource();

        UserRepresentation user = realmResource.users()
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

            Optional<User> user = getUserById(bookingDetails.getUserId());

            if (user.isEmpty()) {
                throw new ResourceNotFoundException("User not found");
            }

            bookingDetails.setUserId(user.get().getId());

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

            Optional<User> user = getUserById(booking.getUserId());

            if (user.isEmpty()) {
                log.warn("User not found  User ID: {}", booking.getUserId());
            }

            LocalDateTime checkOutTime = LocalDateTime.now();
            booking.setCheckOutTime(checkOutTime);
            booking.setUpdatedAt(Timestamp.valueOf(checkOutTime));
            room.setRoomStatus(RoomStatus.READY);
            room.setUpdatedAt(Timestamp.valueOf(checkOutTime));

            bookingRepository.save(booking);
            roomRepository.save(room);

            bookingMailService.sendEmailCheckOut(
                    booking.getOrderId(),
                    user.get().getEmail(),
                    checkOutTime.toString());

            CheckOutResponse response = new CheckOutResponse(
                    booking.getOrderId(),
                    booking.getRoom().getId(),
                    booking.getCheckInTime(),
                    checkOutTime,
                    user.get().getEmail());

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

    public Optional<User> getUserById(String userId) {
        try {
            RealmResource realmResource = keycloakConfig.getRealmResource();
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userId);

            try {
                UserRepresentation userRepresentation = userResource.toRepresentation();
                return Optional.of(new User(userRepresentation));
            } catch (RuntimeException e) {
                log.warn("User with ID {} not found in Keycloak", userId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error while fetching user from Keycloak", e);
            return Optional.empty();
        }
    }

    public Optional<User> getUserByEmail(String email) {
        RealmResource realmResource = keycloakConfig.getRealmResource();
        UsersResource usersResource = realmResource.users();

        var usersByEmail = usersResource.searchByEmail(email, true);

        if (usersByEmail.isEmpty()) {
            return Optional.empty();
        }

        var userRepresentation = usersByEmail.get(0);
        return Optional.of(new User(userRepresentation));
    }
}