package com.justine.service;

import com.justine.dtos.request.BookingRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.BookingResponseDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    ResponseEntity<BookingResponseDTO> createBooking(BookingRequestDTO dto, Long currentUserId);

    ResponseEntity<BookingResponseDTO> getBooking(Long bookingId, Long currentUserId);

    ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(Long guestId, Long currentUserId);

    ResponseEntity<BookingResponseDTO> addServicesToBooking(Long bookingId, List<Long> serviceIds, Long currentUserId);

    ResponseEntity<PaymentResponseDTO> makePayment(PaymentRequestDTO dto, Long currentUserId);

    ResponseEntity<BookingResponseDTO> cancelBooking(Long bookingId, Long currentUserId);

    ResponseEntity<BookingResponseDTO> findByBookingCode(String code, Long currentUserId);

    ResponseEntity<List<RoomResponseDTO>> findAvailableRooms(LocalDate startDate, LocalDate endDate);

    ResponseEntity<Void> autoReleaseRoomsAfterCheckout();

    // ✅ Get all bookings (admin/staff)
    ResponseEntity<List<BookingResponseDTO>> getAllBookings(Long currentUserId);

    // ✅ Filter bookings by date range and status
    ResponseEntity<List<BookingResponseDTO>> filterBookings(LocalDate startDate, LocalDate endDate, String status, Long currentUserId);

    // ✅ Check-in a guest
    ResponseEntity<BookingResponseDTO> checkIn(Long bookingId, Long currentUserId);

    // ✅ Check-out a guest
    ResponseEntity<BookingResponseDTO> checkOut(Long bookingId, Long currentUserId);

    // ✅ Generate invoice for a booking
    ResponseEntity<InvoiceResponseDTO> generateInvoice(Long bookingId, Long currentUserId);


}
