package com.justine.serviceImpl;

import com.justine.dtos.request.BookingRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.*;
import com.justine.enums.BookingStatus;
import com.justine.enums.PaymentStatus;
import com.justine.model.*;
import com.justine.repository.*;
import com.justine.service.AuditLogService;
import com.justine.service.BookingService;
import com.justine.utils.CloudinaryService;
import com.justine.utils.InvoicePdfGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final ServiceRepository serviceRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;

    // ------------------ Auth Helpers ------------------
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String getCurrentPrincipalName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    // ------------------ Create Booking ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> createBooking(BookingRequestDTO dto, String currentUserEmail) {
        try {
            // Get authenticated user info
            String principal = getCurrentPrincipalName();
            boolean adminOrStaff = isAdmin();

            // Fetch the guest for whom the booking is being created
            Guest guest = guestRepository.findById(dto.getGuestId())
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // If a guest is booking, ensure they are booking for themselves
            if (!adminOrStaff && (principal == null || !principal.equalsIgnoreCase(guest.getEmail()))) {
                log.warn("Unauthorized booking attempt by {}", principal);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get room and validate availability
            Room room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (!room.isAvailable()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            // Create booking
            Booking booking = Booking.builder()
                    .bookingCode(UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                    .checkInDate(dto.getCheckInDate())
                    .checkOutDate(dto.getCheckOutDate())
                    .numberOfGuests(dto.getNumberOfGuests())
                    .status(BookingStatus.BOOKED)
                    .guest(guest)
                    .room(room)
                    .build();

            // Optional: Assign staff if admin/staff user created it
            if (adminOrStaff) {
                Staff staff = staffRepository.findByEmail(principal)
                        .orElseThrow(() -> new RuntimeException("Staff not found for current user"));
                booking.setStaff(staff);
            }

            // Add optional services
            if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
                List<com.justine.model.Service> services = serviceRepository.findAllById(dto.getServiceIds());
                booking.setServices(new ArrayList<>(services));
            }

            // Update room availability
            room.setAvailable(false);
            roomRepository.save(room);

            Booking saved = bookingRepository.save(booking);

            // Log who performed the booking
            if (adminOrStaff) {
                auditLogService.logBooking(
                        booking.getStaff().getId(),
                        "CREATE_BOOKING_BY_STAFF",
                        saved.getId(),
                        Map.of("roomId", room.getId(),
                                "guestId", guest.getId(),
                                "checkInDate", dto.getCheckInDate(),
                                "checkOutDate", dto.getCheckOutDate())
                );
            } else {
                auditLogService.logBooking(
                        guest.getId(),
                        "CREATE_BOOKING_BY_GUEST",
                        saved.getId(),
                        Map.of("roomId", room.getId(),
                                "checkInDate", dto.getCheckInDate(),
                                "checkOutDate", dto.getCheckOutDate())
                );
            }

            log.info("Booking created successfully: {} by {}", saved.getBookingCode(), principal);
            return ResponseEntity.status(HttpStatus.CREATED).body(toBookingResponse(saved));

        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);
            auditLogService.logBooking(
                    null,
                    "CREATE_BOOKING_ERROR",
                    null,
                    Map.of("error", e.getMessage(), "request", dto)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ------------------ Get Booking ------------------
    @Override
    public ResponseEntity<BookingResponseDTO> getBooking(Long bookingId, String currentUserEmail) {
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) return ResponseEntity.ok(null);

            Booking booking = bookingOpt.get();
            String principal = getCurrentPrincipalName();

            if (!isAdmin() && (principal == null || !principal.equals(booking.getGuest().getEmail()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "VIEW_BOOKING_SUCCESS",
                    booking.getId(),
                    Map.of("bookingCode", booking.getBookingCode())
            );

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Error getting booking: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "VIEW_BOOKING_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ List Bookings ------------------
    @Override
    public ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(Long guestId, String currentUserEmail) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            String principal = getCurrentPrincipalName();
            if (!isAdmin() && (principal == null || !principal.equals(guest.getEmail()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<BookingResponseDTO> bookings = bookingRepository.findByGuestId(guestId).stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            auditLogService.logBooking(guestId, "LIST_BOOKINGS_SUCCESS", null, Map.of("count", bookings.size()));
            return ResponseEntity.ok(bookings.isEmpty() ? Collections.emptyList() : bookings);
        } catch (Exception e) {
            log.error("Error listing bookings: {}", e.getMessage());
            auditLogService.logBooking(
                    guestId,
                    "LIST_BOOKINGS_ERROR",
                    null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ------------------ Add Services to Booking ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> addServicesToBooking(Long bookingId, List<Long> serviceIds, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            String principal = getCurrentPrincipalName();
            if (!isAdmin() && (principal == null || !principal.equals(booking.getGuest().getEmail()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<com.justine.model.Service> services = serviceRepository.findAllById(serviceIds);
            if (booking.getServices() == null) booking.setServices(new ArrayList<>());
            booking.getServices().addAll(services);

            Booking saved = bookingRepository.save(booking);

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "ADD_SERVICES_SUCCESS",
                    booking.getId(),
                    Map.of("addedServices", serviceIds)
            );

            return ResponseEntity.ok(toBookingResponse(saved));
        } catch (Exception e) {
            log.error("Error adding services: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "ADD_SERVICES_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage(), "serviceIds", serviceIds)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Make Payment ------------------
    @Override
    @Transactional
    public ResponseEntity<PaymentResponseDTO> makePayment(PaymentRequestDTO dto, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            Payment payment = Payment.builder()
                    .amount(dto.getAmount())
                    .method(dto.getMethod())
                    .status(PaymentStatus.PENDING)
                    .paymentDate(LocalDateTime.now())
                    .transactionId(dto.getTransactionId())
                    .booking(booking)
                    .build();

            paymentRepository.save(payment);
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);

            Invoice invoice = Invoice.builder()
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .issuedDate(LocalDate.now())
                    .totalAmount(dto.getAmount())
                    .paid(true)
                    .booking(booking)
                    .build();

            invoiceRepository.save(invoice);
            booking.setPayment(payment);
            booking.setInvoice(invoice);
            bookingRepository.save(booking);

            try {
                File pdf = InvoicePdfGenerator.generateReceipt(invoice, System.getProperty("java.io.tmpdir"));
                String url = cloudinaryService.uploadFile(pdf, "hotel_receipts");
                invoice.setInvoiceUrl(url);
                invoiceRepository.save(invoice);
                pdf.delete();
            } catch (Exception ex) {
                log.error("Invoice upload failed: {}", ex.getMessage());
            }

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "MAKE_PAYMENT_SUCCESS",
                    booking.getId(),
                    Map.of("amount", dto.getAmount(), "transactionId", dto.getTransactionId())
            );

            return ResponseEntity.ok(toPaymentResponse(payment));
        } catch (Exception e) {
            log.error("Payment error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "MAKE_PAYMENT_ERROR",
                    dto.getBookingId(),
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Cancel Booking ------------------
    @Override
    @Transactional
    public ResponseEntity<Void> cancelBooking(Long bookingId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(true);
                roomRepository.save(room);
            }

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "CANCEL_BOOKING_SUCCESS",
                    booking.getId(),
                    Map.of("roomId", room != null ? room.getId() : null)
            );

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Cancel booking error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "CANCEL_BOOKING_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Search Booking by Code ------------------
    @Override
    public ResponseEntity<BookingResponseDTO> findByBookingCode(String code, String currentUserEmail) {
        try {
            Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(code);
            if (bookingOpt.isEmpty()) return ResponseEntity.ok(null);

            Booking booking = bookingOpt.get();

            auditLogService.logBooking(
                    booking.getGuest() != null ? booking.getGuest().getId() : null,
                    "SEARCH_BOOKING_SUCCESS",
                    booking.getId(),
                    Map.of("bookingCode", code)
            );

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Search booking error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "SEARCH_BOOKING_ERROR",
                    null,
                    Map.of("error", e.getMessage(), "bookingCode", code)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Auto Release Rooms ------------------
    @Override
    @Transactional
    public ResponseEntity<Void> autoReleaseRoomsAfterCheckout() {
        try {
            LocalDate today = LocalDate.now();
            List<Booking> bookings = bookingRepository.findAll();

            bookings.stream()
                    .filter(b -> b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today))
                    .forEach(b -> {
                        Room room = b.getRoom();
                        if (room != null && !room.isAvailable()) {
                            room.setAvailable(true);
                            roomRepository.save(room);
                            auditLogService.logBooking(
                                    b.getGuest() != null ? b.getGuest().getId() : null,
                                    "AUTO_RELEASE_ROOM_SUCCESS",
                                    b.getId(),
                                    Map.of("roomId", room.getId(), "checkOutDate", b.getCheckOutDate())
                            );
                        }
                    });

            log.info("Auto room release complete");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Auto release error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "AUTO_RELEASE_ROOM_ERROR",
                    null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Find Available Rooms ------------------
    @Override
    public ResponseEntity<List<RoomResponseDTO>> findAvailableRooms(LocalDate startDate, LocalDate endDate) {
        try {
            List<Booking> overlapping = bookingRepository.findOverlappingBookings(startDate, endDate);
            Set<Long> occupiedRoomIds = overlapping.stream()
                    .map(b -> b.getRoom().getId())
                    .collect(Collectors.toSet());

            List<RoomResponseDTO> availableRooms = roomRepository.findAll().stream()
                    .filter(r -> !occupiedRoomIds.contains(r.getId()) && r.isAvailable())
                    .map(r -> RoomResponseDTO.builder()
                            .id(r.getId())
                            .roomNumber(r.getRoomNumber())
                            .pricePerNight(r.getPricePerNight())
                            .type(r.getType())
                            .available(r.isAvailable())
                            .build())
                    .collect(Collectors.toList());

            auditLogService.logBooking(
                    null,
                    "FIND_AVAILABLE_ROOMS_SUCCESS",
                    null,
                    Map.of("count", availableRooms.size())
            );

            return ResponseEntity.ok(availableRooms.isEmpty() ? Collections.emptyList() : availableRooms);
        } catch (Exception e) {
            log.error("Error finding rooms: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "FIND_AVAILABLE_ROOMS_ERROR",
                    null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ------------------ Get All Bookings ------------------
    @Override
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings(String currentUserEmail) {
        try {
            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<BookingResponseDTO> bookings = bookingRepository.findAll().stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            auditLogService.logBooking(
                    null,
                    "GET_ALL_BOOKINGS_SUCCESS",
                    null,
                    Map.of("count", bookings.size())
            );

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error fetching all bookings: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "GET_ALL_BOOKINGS_ERROR",
                    null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }


    // ------------------ Filter Bookings ------------------
    @Override
    public ResponseEntity<List<BookingResponseDTO>> filterBookings(LocalDate startDate, LocalDate endDate, String status, String currentUserEmail) {
        try {
            List<Booking> bookings = bookingRepository.findAll();

            // Apply filters
            if (startDate != null) {
                bookings = bookings.stream()
                        .filter(b -> !b.getCheckInDate().isBefore(startDate))
                        .collect(Collectors.toList());
            }
            if (endDate != null) {
                bookings = bookings.stream()
                        .filter(b -> !b.getCheckOutDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }
            if (status != null && !status.isBlank()) {
                bookings = bookings.stream()
                        .filter(b -> b.getStatus() != null && b.getStatus().name().equalsIgnoreCase(status))
                        .collect(Collectors.toList());
            }

            List<BookingResponseDTO> result = bookings.stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            auditLogService.logBooking(
                    null,
                    "FILTER_BOOKINGS_SUCCESS",
                    null,
                    Map.of(
                            "startDate", startDate,
                            "endDate", endDate,
                            "status", status,
                            "count", result.size()
                    )
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error filtering bookings: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "FILTER_BOOKINGS_ERROR",
                    null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }


    // ------------------ Check-In ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> checkIn(Long bookingId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            booking.setStatus(BookingStatus.CHECKED_IN);
            bookingRepository.save(booking);

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "CHECK_IN_SUCCESS",
                    booking.getId(),
                    Map.of("bookingCode", booking.getBookingCode())
            );

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Check-in error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "CHECK_IN_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ------------------ Check-Out ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> checkOut(Long bookingId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            booking.setStatus(BookingStatus.CHECKED_OUT);

            // Mark room as available again
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(true);
                roomRepository.save(room);
            }

            bookingRepository.save(booking);

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "CHECK_OUT_SUCCESS",
                    booking.getId(),
                    Map.of("roomId", room != null ? room.getId() : null)
            );

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Check-out error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "CHECK_OUT_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ------------------ Generate Invoice ------------------
    @Override
    @Transactional
    public ResponseEntity<Void> generateInvoice(Long bookingId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            double totalAmount = booking.getRoom().getPricePerNight();
            if (booking.getServices() != null) {
                totalAmount += booking.getServices().stream()
                        .mapToDouble(s -> s.getPrice() != null ? s.getPrice() : 0.0)
                        .sum();
            }

            Invoice invoice = Invoice.builder()
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .issuedDate(LocalDate.now())
                    .totalAmount(totalAmount)
                    .paid(false)
                    .booking(booking)
                    .build();

            invoiceRepository.save(invoice);

            try {
                File pdf = InvoicePdfGenerator.generateReceipt(invoice, System.getProperty("java.io.tmpdir"));
                String url = cloudinaryService.uploadFile(pdf, "hotel_invoices");
                invoice.setInvoiceUrl(url);
                invoiceRepository.save(invoice);
                pdf.delete();
            } catch (Exception ex) {
                log.error("Invoice PDF generation/upload failed: {}", ex.getMessage());
            }

            booking.setInvoice(invoice);
            bookingRepository.save(booking);

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "GENERATE_INVOICE_SUCCESS",
                    booking.getId(),
                    Map.of("invoiceNumber", invoice.getInvoiceNumber(), "totalAmount", totalAmount)
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Generate invoice error: {}", e.getMessage());
            auditLogService.logBooking(
                    null,
                    "GENERATE_INVOICE_ERROR",
                    bookingId,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ------------------ Mapping Helpers ------------------
    private BookingResponseDTO toBookingResponse(Booking booking) {
        BookingResponseDTO dto = BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .build();

        if (booking.getGuest() != null) {
            Guest g = booking.getGuest();
            dto.setGuest(GuestResponseDTO.builder()
                    .id(g.getId())
                    .fullName(g.getFullName())
                    .email(g.getEmail())
                    .phoneNumber(g.getPhoneNumber())
                    .idNumber(g.getIdNumber())
                    .role(g.getRole())
                    .build());
        }

        if (booking.getRoom() != null) {
            Room r = booking.getRoom();
            dto.setRoom(RoomResponseDTO.builder()
                    .id(r.getId())
                    .roomNumber(r.getRoomNumber())
                    .type(r.getType())
                    .pricePerNight(r.getPricePerNight())
                    .available(r.isAvailable())
                    .build());
        }

        if (booking.getPayment() != null) dto.setPayment(toPaymentResponse(booking.getPayment()));
        if (booking.getInvoice() != null) {
            Invoice inv = booking.getInvoice();
            dto.setInvoice(InvoiceResponseDTO.builder()
                    .id(inv.getId())
                    .invoiceNumber(inv.getInvoiceNumber())
                    .issuedDate(inv.getIssuedDate())
                    .totalAmount(inv.getTotalAmount())
                    .paid(inv.isPaid())
                    .invoiceUrl(inv.getInvoiceUrl())
                    .build());
        }

        if (booking.getServices() != null) {
            dto.setServices(booking.getServices().stream()
                    .map(s -> ServiceResponseDTO.builder()
                            .id(s.getId())
                            .serviceType(s.getServiceType())
                            .price(s.getPrice())
                            .build())
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private PaymentResponseDTO toPaymentResponse(Payment p) {
        return PaymentResponseDTO.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .method(p.getMethod())
                .status(p.getStatus())
                .paymentDate(p.getPaymentDate())
                .transactionId(p.getTransactionId())
                .build();
    }
}
