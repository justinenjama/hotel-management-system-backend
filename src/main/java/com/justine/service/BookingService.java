package com.justine.service;

import com.justine.dtos.request.BookingRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.BookingResponseDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    ResponseEntity<BookingResponseDTO> createBooking(BookingRequestDTO dto, String currentUserEmail);

    ResponseEntity<BookingResponseDTO> getBooking(Long bookingId, String currentUserEmail);

    ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(Long guestId, String currentUserEmail);

    ResponseEntity<BookingResponseDTO> addServicesToBooking(Long bookingId, List<Long> serviceIds, String currentUserEmail);

    ResponseEntity<PaymentResponseDTO> makePayment(PaymentRequestDTO dto, String currentUserEmail);

    ResponseEntity<Void> cancelBooking(Long bookingId, String currentUserEmail);

    ResponseEntity<BookingResponseDTO> findByBookingCode(String code, String currentUserEmail);

    ResponseEntity<List<RoomResponseDTO>> findAvailableRooms(LocalDate startDate, LocalDate endDate);

    ResponseEntity<Void> autoReleaseRoomsAfterCheckout();

    // ✅ Get all bookings (admin/staff)
    ResponseEntity<List<BookingResponseDTO>> getAllBookings(String currentUserEmail);

    // ✅ Filter bookings by date range and status
    ResponseEntity<List<BookingResponseDTO>> filterBookings(LocalDate startDate, LocalDate endDate, String status, String currentUserEmail);

    // ✅ Check-in a guest
    ResponseEntity<BookingResponseDTO> checkIn(Long bookingId, String currentUserEmail);

    // ✅ Check-out a guest
    ResponseEntity<BookingResponseDTO> checkOut(Long bookingId, String currentUserEmail);

    // ✅ Generate invoice for a booking
    ResponseEntity<Void> generateInvoice(Long bookingId, String currentUserEmail);

}
