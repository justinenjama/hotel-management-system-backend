package com.justine.controller;

import com.justine.dtos.request.BookingRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.BookingResponseDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
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
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // --- Utility method to extract userId from Principal ---
    private Long extractUserId(Principal principal) {
        try {
            return (principal != null) ? Long.parseLong(principal.getName()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO dto, Principal principal) {
        return bookingService.createBooking(dto, extractUserId(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBooking(@PathVariable Long id, Principal principal) {
        return bookingService.getBooking(id, extractUserId(principal));
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(@PathVariable Long guestId, Principal principal) {
        return bookingService.listBookingsForGuest(guestId, extractUserId(principal));
    }

    @PostMapping("/{id}/services")
    public ResponseEntity<BookingResponseDTO> addServices(
            @PathVariable Long id,
            @RequestBody List<Long> serviceIds,
            Principal principal
    ) {
        return bookingService.addServicesToBooking(id, serviceIds, extractUserId(principal));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponseDTO> pay(@RequestBody PaymentRequestDTO dto, Principal principal) {
        return bookingService.makePayment(dto, extractUserId(principal));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancel(@PathVariable Long id, Principal principal) {
        return bookingService.cancelBooking(id, extractUserId(principal));
    }

    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<BookingResponseDTO> getByBookingCode(@PathVariable String bookingCode, Principal principal) {
        return bookingService.findByBookingCode(bookingCode, extractUserId(principal));
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponseDTO>> getAvailableRooms(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return bookingService.findAvailableRooms(start, end);
    }

    @PostMapping("/auto-release")
    public ResponseEntity<Void> autoReleaseRooms() {
        return bookingService.autoReleaseRoomsAfterCheckout();
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings(Principal principal) {
        return bookingService.getAllBookings(extractUserId(principal));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<BookingResponseDTO>> filterBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            Principal principal
    ) {
        return bookingService.filterBookings(startDate, endDate, status, extractUserId(principal));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<BookingResponseDTO> checkIn(@PathVariable Long id, Principal principal) {
        return bookingService.checkIn(id, extractUserId(principal));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<BookingResponseDTO> checkOut(@PathVariable Long id, Principal principal) {
        return bookingService.checkOut(id, extractUserId(principal));
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<InvoiceResponseDTO> generateInvoice(@PathVariable Long id, Principal principal) {
        return bookingService.generateInvoice(id, extractUserId(principal));
    }
}
