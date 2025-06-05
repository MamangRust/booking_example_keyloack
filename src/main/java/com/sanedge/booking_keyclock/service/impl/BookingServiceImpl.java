package com.sanedge.booking_keyclock.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sanedge.booking_keyclock.config.KeycloakConfig;
import com.sanedge.booking_keyclock.domain.request.booking.CreateBookingRequest;
import com.sanedge.booking_keyclock.domain.request.booking.UpdateBookingRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.enums.RoomStatus;
import com.sanedge.booking_keyclock.models.Booking;
import com.sanedge.booking_keyclock.models.Room;
import com.sanedge.booking_keyclock.models.User;
import com.sanedge.booking_keyclock.repository.BookingRepository;
import com.sanedge.booking_keyclock.repository.RoomRepository;
import com.sanedge.booking_keyclock.service.BookingMailService;
import com.sanedge.booking_keyclock.service.BookingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {
    private final KeycloakConfig keycloakConfig;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingMailService bookingMailService;

    @Autowired
    public BookingServiceImpl(RoomRepository roomRepository,
            BookingRepository bookingRepository, BookingMailService bookingMailService, KeycloakConfig keycloakConfig) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMailService = bookingMailService;
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public MessageResponse findAll() {
        try {
            log.info("Fetching all bookings");
            List<Booking> bookingList = this.bookingRepository.findAll();

            log.info("Found {} bookings", bookingList.size());

            return MessageResponse.builder()
                    .message("Booking data retrieved successfully")
                    .data(bookingList)
                    .statusCode(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            log.error("Error fetching all bookings", e);

            return MessageResponse.builder()
                    .message("Error fetching all bookings")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    @Override
    public MessageResponse findById(Long id) {
        try {
            log.info("Fetching booking by id: {}", id);

            Booking findBooking = this.bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Not found booking"));

            log.info("Found booking: {}", findBooking);

            return MessageResponse.builder().message("Success").data(findBooking).statusCode(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            log.error("Error fetching booking by id: {}", id, e);

            return MessageResponse.builder()
                    .message("Error fetching booking")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    @Override
    public MessageResponse createBooking(String userId, CreateBookingRequest request) {
        try {
            Optional<User> userOptional = this.getUserById(userId);

            if (userOptional.isEmpty()) {
                log.warn("User not found  User ID: {}", userId);
            }

            User user = userOptional.get();
            String userName = user.getUsername();

            log.info("Creating new booking for user {} and room {}", userName,
                    request.getRoomId());

            Room findRoom = this.roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Not found room"));

            if (request.getTotalPerson() >= findRoom.getRoomCapacity()) {
                return MessageResponse.builder()
                        .message("Room capacity not enough")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
            }

            if (findRoom.getRoomStatus() == RoomStatus.BOOKING) {
                return MessageResponse.builder()
                        .message("Room is booking")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
            }

            Booking orderBooking = new Booking();

            orderBooking.setOrderId("ORDER" + System.currentTimeMillis());
            orderBooking.setUserId(userOptional.get().getId());
            orderBooking.setUserEmail(userOptional.get().getEmail());    
            orderBooking.setUsername(userOptional.get().getUsername());
            orderBooking.setRoom(findRoom);
            orderBooking.setTotalPerson(request.getTotalPerson());
            orderBooking.setBookingTime(request.getBookingTime());
            orderBooking.setNoted(request.getNoted());
            orderBooking.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            this.bookingRepository.save(orderBooking);

            log.info("Booking created successfully: {}", orderBooking);

            findRoom.setRoomStatus(RoomStatus.BOOKING);

            this.roomRepository.save(findRoom);

            return MessageResponse.builder().message("Success").data(orderBooking).statusCode(200).build();
        } catch (Exception e) {
            log.error("Error creating booking for user {} and room {}", userId, request.getRoomId(), e);

            return MessageResponse.builder()
                    .message("Error creating booking")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    @Override
    public MessageResponse updateBooking(Long id, String userId, UpdateBookingRequest request) {
        try {
            Optional<User> userOptional = this.getUserById(userId);

            if (userOptional.isEmpty()) {
                log.warn("User not found  User ID: {}", userId);
            }

            log.info("Updating booking with id: {}", id);

            Room findRoom = this.roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Not found room"));

            Booking findBooking = this.bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Not found booking"));

            if (request.getTotalPerson() >= findRoom.getRoomCapacity()) {
                return MessageResponse.builder()
                        .message("Room capacity not enough")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
            }

            if (findRoom.getRoomStatus() == RoomStatus.BOOKING) {
                return MessageResponse.builder()
                        .message("Room is booking")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
            }

            findBooking.setRoom(findRoom);
            findBooking.setUserId(userOptional.get().getId());
            findBooking.setUserEmail(userOptional.get().getEmail());    
            findBooking.setUsername(userOptional.get().getUsername());
            findBooking.setTotalPerson(request.getTotalPerson());
            findBooking.setBookingTime(request.getBookingTime());
            findBooking.setNoted(request.getNoted());

            this.bookingRepository.save(findBooking);

            log.info("Booking updated successfully: {}", findBooking);

            return MessageResponse.builder().message("Booking updated successfully").data(findBooking)
                    .statusCode(HttpStatus.OK.value()).build();
        } catch (Exception e) {
            log.error("Error updating booking with id: {}", id, e);

            return MessageResponse.builder().message("Error updating booking")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
        }
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Jakarta")
    public void bookingTimeCronJob() {
        try {
            LocalDateTime dateNow = LocalDateTime.now();

            List<Booking> bookings = bookingRepository.findBookingsByBookingTime(dateNow);

            for (Booking booking : bookings) {
                String userId = booking.getUserId();

                Optional<User> userOptional = this.getUserById(userId);

                if (userOptional.isEmpty()) {
                    log.warn("User not found for booking {} (User ID: {})", booking.getId(), userId);
                    return;
                }

                User user = userOptional.get();
                String userEmail = user.getEmail();

                if (userEmail == null || userEmail.isBlank()) {
                    log.warn("No email found for user {} (Booking: {})", user.getUsername(), booking.getOrderId());
                    return;
                }

                bookingMailService.sendEmailBookingTime(booking.getOrderId(), userEmail, booking.getCheckInTime());
                log.info("Booking time cron job sent email to: {}", userEmail);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error booking time cron job", e);
        }
    }

    @Override
    public MessageResponse deleteById(Long id) {
        try {
            log.info("Deleting booking with id: {}", id);

            Booking findBooking = this.bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Not found booking"));

            this.bookingRepository.delete(findBooking);

            log.info("Booking deleted successfully");

            return MessageResponse.builder().message("Success").statusCode(HttpStatus.OK.value()).build();
        } catch (Exception e) {
            log.error("Error deleting booking with id: {}", id, e);

            return MessageResponse.builder().message("Error deleting booking")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
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

}
