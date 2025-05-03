package com.sanedge.booking_keyclock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.booking_keyclock.domain.response.booking.BookingDetailsResponse;
import com.sanedge.booking_keyclock.domain.response.booking.CheckOutResponse;
import com.sanedge.booking_keyclock.models.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
        Optional<Booking> findByOrderId(String orderId);

        List<Booking> findByUserId(String userId);

        @Query("SELECT new com.sanedge.booking_keyclock.domain.response.booking.BookingDetailsResponse(" +
                        "b.orderId, b.totalPerson, b.bookingTime, b.userId, " +
                        "r.roomName, r.roomCapacity, r.photo, r.roomStatus) " +
                        "FROM Booking b JOIN b.room r WHERE b.orderId = :orderId")
        Optional<BookingDetailsResponse> findBookingDetailsByOrderId(@Param("orderId") String orderId);

        @Query("SELECT new com.sanedge.booking_keyclock.domain.response.booking.CheckOutResponse(" +
                        "b.orderId, b.room.id, b.checkOutTime, b.userId) " +
                        "FROM Booking b " +
                        "WHERE b.orderId = :orderId")
        Optional<CheckOutResponse> findCheckoutDetailsByOrderId(String orderId);

        @Query("SELECT b FROM Booking b WHERE b.bookingTime = :dateNow")
        List<Booking> findBookingsByBookingTime(@Param("dateNow") LocalDateTime dateNow);
}