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

    // Fetch bookings for a specific guest with guest, room, services loaded
    @EntityGraph(attributePaths = {"guest", "room", "services"})
    List<Booking> findByGuestId(Long guestId);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Booking> findByBookingCode(String bookingCode);

    // Check for overlapping bookings
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

    // Fetch all bookings with related entities
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.guest " +
            "LEFT JOIN FETCH b.room " +
            "LEFT JOIN FETCH b.services " +
            "LEFT JOIN FETCH b.payment")
    List<Booking> findAllWithPayment();

    @Override
    @EntityGraph(attributePaths = {"guest", "room", "services", "payment"})
    List<Booking> findAll();

    // Active bookings on a specific date
    List<Booking> findByCheckInDateBeforeAndCheckOutDateAfter(LocalDate today, LocalDate today1);

    // Active bookings for a hotel
    @Query("SELECT b FROM Booking b " +
            "WHERE b.room.hotel.id = :hotelId " +
            "AND b.checkInDate <= :today " +
            "AND b.checkOutDate >= :today")
    List<Booking> findActiveBookingsByHotel(@Param("hotelId") Long hotelId, @Param("today") LocalDate today);

    List<Booking> findByCheckOutDateBeforeOrCheckOutDateEqualsAndStatusNot(LocalDate today, LocalDate today1, BookingStatus bookingStatus);

    @EntityGraph(attributePaths = {"guest", "room", "services", "invoice"})
    List<Booking> findByStaffId(Long staffId);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.guest " +
            "LEFT JOIN FETCH b.room " +
            "LEFT JOIN FETCH b.services " +
            "LEFT JOIN FETCH b.invoice " +
            "WHERE b.staff.id = :staffId")
    List<Booking> findBookingsWithInvoiceByStaffId(@Param("staffId") Long staffId);

}

