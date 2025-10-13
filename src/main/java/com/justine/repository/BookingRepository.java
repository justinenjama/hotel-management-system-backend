package com.justine.repository;

import com.justine.model.Booking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestId(Long guestId);

    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("""
        SELECT b FROM Booking b
        WHERE (b.checkInDate BETWEEN :start AND :end)
           OR (b.checkOutDate BETWEEN :start AND :end)
           OR (:start BETWEEN b.checkInDate AND b.checkOutDate)
           OR (:end BETWEEN b.checkInDate AND b.checkOutDate)
    """)
    List<Booking> findOverlappingBookings(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
