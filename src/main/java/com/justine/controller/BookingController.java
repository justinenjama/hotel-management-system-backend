package com.justine.controller;

import com.justine.dtos.request.BookingRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.BookingResponseDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import com.justine.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO dto, Principal principal) {
        return bookingService.createBooking(dto, principal == null ? null : principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBooking(@PathVariable Long id, Principal principal) {
        return bookingService.getBooking(id, principal == null ? null : principal.getName());
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(@PathVariable Long guestId, Principal principal) {
        return bookingService.listBookingsForGuest(guestId, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/services")
    public ResponseEntity<BookingResponseDTO> addServices(@PathVariable Long id, @RequestBody List<Long> serviceIds, Principal principal) {
        return bookingService.addServicesToBooking(id, serviceIds, principal == null ? null : principal.getName());
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponseDTO> pay(@RequestBody PaymentRequestDTO dto, Principal principal) {
        return bookingService.makePayment(dto, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Principal principal) {
        return bookingService.cancelBooking(id, principal == null ? null : principal.getName());
    }

    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<BookingResponseDTO> getByBookingCode(@PathVariable String bookingCode, Principal principal) {
        return bookingService.findByBookingCode(bookingCode, principal == null ? null : principal.getName());
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponseDTO>> getAvailableRooms(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return bookingService.findAvailableRooms(start, end);
    }

    @PostMapping("/auto-release")
    public ResponseEntity<Void> autoReleaseRooms() {
        return bookingService.autoReleaseRoomsAfterCheckout();
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings(Principal principal) {
        return bookingService.getAllBookings(principal == null ? null : principal.getName());
    }

    @GetMapping("/filter")
    public ResponseEntity<List<BookingResponseDTO>> filterBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            Principal principal
    ) {
        return bookingService.filterBookings(startDate, endDate, status, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<BookingResponseDTO> checkIn(@PathVariable Long id, Principal principal) {
        return bookingService.checkIn(id, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<BookingResponseDTO> checkOut(@PathVariable Long id, Principal principal) {
        return bookingService.checkOut(id, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<Void> generateInvoice(@PathVariable Long id, Principal principal) {
        return bookingService.generateInvoice(id, principal == null ? null : principal.getName());
    }
}
