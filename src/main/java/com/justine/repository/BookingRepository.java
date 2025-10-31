package com.justine.repository;

import com.justine.enums.BookingStatus;
import com.justine.model.Booking;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"guest", "room", "services"})
    List<Booking> findByGuestId(Long guestId);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("""
        SELECT b FROM Booking b
        WHERE (b.checkInDate BETWEEN :start AND :end)
           OR (b.checkOutDate BETWEEN :start AND :end)
           OR (:start BETWEEN b.checkInDate AND b.checkOutDate)
           OR (:end BETWEEN b.checkInDate AND b.checkOutDate)
    """)
    List<Booking> findOverlappingBookings(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Booking> findByGuestIdAndCheckInDateBetweenAndStatus(Long guestId, LocalDate start, LocalDate end, BookingStatus status);
    List<Booking> findByGuestIdAndCheckInDateBetween(Long guestId, LocalDate start, LocalDate end);
    List<Booking> findByCheckInDateBetween(LocalDate start, LocalDate end);
    List<Booking> findByCheckInDateBetweenAndStatus(LocalDate start, LocalDate end, BookingStatus status);
}
