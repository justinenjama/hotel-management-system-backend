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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
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

    public BookingServiceImpl(BookingRepository bookingRepository, RoomRepository roomRepository, GuestRepository guestRepository, ServiceRepository serviceRepository, InvoiceRepository invoiceRepository, PaymentRepository paymentRepository, CloudinaryService cloudinaryService, AuditLogService auditLogService, StaffRepository staffRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.serviceRepository = serviceRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.cloudinaryService = cloudinaryService;
        this.auditLogService = auditLogService;
        this.staffRepository = staffRepository;
    }

    // ------------------ Auth Helpers ------------------
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse current user ID from principal: {}", auth.getName());
            return null;
        }
    }
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> createBooking(BookingRequestDTO dto, Long currentUserId) {
        try {
            boolean adminOrStaff = isAdmin();

            Guest guest = guestRepository.findById(dto.getGuestId())
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            if (!adminOrStaff && (currentUserId == null || !currentUserId.equals(guest.getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Room room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (!room.isAvailable()) return ResponseEntity.status(HttpStatus.CONFLICT).build();

            Booking booking = Booking.builder()
                    .bookingCode(UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                    .checkInDate(dto.getCheckInDate())
                    .checkOutDate(dto.getCheckOutDate())
                    .numberOfGuests(dto.getNumberOfGuests())
                    .status(BookingStatus.BOOKED)
                    .guest(guest)
                    .room(room)
                    .build();

            if (adminOrStaff) {
                Staff staff = staffRepository.findById(currentUserId)
                        .orElseThrow(() -> new RuntimeException("Staff not found"));
                booking.setStaff(staff);
            }

            if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
                List<com.justine.model.Service> services = serviceRepository.findAllById(dto.getServiceIds());
                booking.setServices(new ArrayList<>(services));
            }

            room.setAvailable(false);
            roomRepository.save(room);

            Booking saved = bookingRepository.save(booking);

            // ---------------- Corrected totalAmount calculation ----------------
            long nights = ChronoUnit.DAYS.between(saved.getCheckInDate(), saved.getCheckOutDate());
            nights = Math.max(nights, 1); // ensure at least 1 night

            double totalAmount = (saved.getRoom().getPricePerNight() != null ? saved.getRoom().getPricePerNight() : 0.0) * nights;

            if (saved.getServices() != null) {
                totalAmount += saved.getServices().stream()
                        .mapToDouble(s -> s.getPrice() != null ? s.getPrice() : 0.0)
                        .sum();
            }
            // -------------------------------------------------------------------

            Invoice invoice = Invoice.builder()
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .issuedDate(LocalDate.now())
                    .totalAmount(totalAmount)
                    .paid(false)
                    .booking(saved)
                    .build();
            invoiceRepository.save(invoice);

            generateAndUploadInvoicePdf(invoice);

            saved.setInvoice(invoice);
            bookingRepository.save(saved);

            auditLogService.logBooking(
                    guest.getId(),
                    "AUTO_GENERATE_INVOICE_SUCCESS",
                    saved.getId(),
                    Map.of("invoiceNumber", invoice.getInvoiceNumber(), "amount", totalAmount)
            );

            if (adminOrStaff) {
                auditLogService.logBooking(
                        booking.getStaff().getId(),
                        "CREATE_BOOKING_BY_STAFF",
                        saved.getId(),
                        Map.of("roomId", room.getId(), "guestId", guest.getId())
                );
            } else {
                auditLogService.logBooking(
                        guest.getId(),
                        "CREATE_BOOKING_BY_GUEST",
                        saved.getId(),
                        Map.of("roomId", room.getId())
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(toBookingResponse(saved));

        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);
            auditLogService.logBooking(null, "CREATE_BOOKING_ERROR", null,
                    Map.of("error", e.getMessage(), "request", dto));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Make Payment ------------------
    @Override
    @Transactional
    public ResponseEntity<PaymentResponseDTO> makePayment(PaymentRequestDTO dto, Long currentUserId) {
        try {
            // 1️⃣ Fetch booking
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // 2️⃣ Prevent paying again if already paid
            if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.PAID) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // 3️⃣ Calculate total cost
            long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            double roomCost = booking.getRoom().getPricePerNight() * nights;

            double serviceCost = 0.0;
            if (booking.getServices() != null && !booking.getServices().isEmpty()) {
                serviceCost = booking.getServices().stream()
                        .mapToDouble(s -> s.getPrice() != null ? s.getPrice() : 0.0)
                        .sum();
            }

            double totalCost = roomCost + serviceCost;

            // 4️⃣ Create or update payment
            Payment payment = booking.getPayment() != null ? booking.getPayment() : new Payment();
            payment.setBooking(booking);
            payment.setAmount(totalCost);
            payment.setMethod(dto.getMethod());
            payment.setTransactionId(dto.getTransactionId());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);
            booking.setPayment(payment);
            booking.setStatus(BookingStatus.CHECKED_IN);

            // 5️⃣ Create or update invoice
            Invoice invoice = booking.getInvoice();
            if (invoice == null) {
                invoice = Invoice.builder()
                        .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .issuedDate(LocalDate.now())
                        .totalAmount(totalCost)
                        .paid(true)
                        .booking(booking)
                        .build();
            } else {
                invoice.setPaid(true);
                invoice.setTotalAmount(totalCost);
                invoice.setIssuedDate(LocalDate.now());
            }
            invoiceRepository.save(invoice);

            // 6️⃣ Generate and upload PDF
            generateAndUploadInvoicePdf(invoice);

            booking.setInvoice(invoice);
            bookingRepository.save(booking);

            // 7️⃣ Audit log
            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "MAKE_PAYMENT_SUCCESS",
                    booking.getId(),
                    Map.of("amount", totalCost, "transactionId", dto.getTransactionId())
            );

            // 8️⃣ Return response
            return ResponseEntity.ok(toPaymentResponse(payment));

        } catch (Exception e) {
            log.error("Payment error", e);
            auditLogService.logBooking(
                    null,
                    "MAKE_PAYMENT_ERROR",
                    dto.getBookingId(),
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ------------------ Generate Invoice ------------------
    @Override
    @Transactional
    public ResponseEntity<InvoiceResponseDTO> generateInvoice(Long bookingId, Long currentUserId) {
        try {
            if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            Invoice invoice = booking.getInvoice();
            if (invoice != null) return ResponseEntity.ok(toInvoiceResponse(invoice));

            double totalAmount = booking.getRoom().getPricePerNight();
            if (booking.getServices() != null) {
                totalAmount += booking.getServices().stream()
                        .mapToDouble(s -> s.getPrice() != null ? s.getPrice() : 0.0)
                        .sum();
            }

            invoice = Invoice.builder()
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .issuedDate(LocalDate.now())
                    .totalAmount(totalAmount)
                    .paid(false)
                    .booking(booking)
                    .build();
            invoiceRepository.save(invoice);

            generateAndUploadInvoicePdf(invoice);

            booking.setInvoice(invoice);
            bookingRepository.save(booking);

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "GENERATE_INVOICE_SUCCESS",
                    booking.getId(),
                    Map.of("invoiceNumber", invoice.getInvoiceNumber(), "totalAmount", totalAmount)
            );

            return ResponseEntity.ok(toInvoiceResponse(invoice));

        } catch (Exception e) {
            log.error("Generate invoice error: {}", e.getMessage(), e);
            auditLogService.logBooking(null, "GENERATE_INVOICE_ERROR", bookingId,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Helper: PDF & Cloud Upload ------------------
    private void generateAndUploadInvoicePdf(Invoice invoice) {
        try {
            MultipartFile pdfFile = InvoicePdfGenerator.generateReceipt(invoice);
            Map<String, String> urls = cloudinaryService.uploadFileWithEagerSizes(pdfFile, "hotel_invoices");
            invoice.setInvoiceUrl(urls.get("large"));
            invoice.setInvoiceUrlMedium(urls.get("medium"));
            invoice.setInvoiceUrlThumbnail(urls.get("thumbnail"));
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.error("Invoice PDF generation/upload failed for invoice {}: {}", invoice.getId(), e.getMessage(), e);
        }
    }

    // ------------------ Get Booking ------------------
    @Override
    public ResponseEntity<BookingResponseDTO> getBooking(Long bookingId, Long currentUserId) {
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) return ResponseEntity.ok(null);

            Booking booking = bookingOpt.get();

            if (!isAdmin() && (currentUserId == null || !currentUserId.equals(booking.getGuest().getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logBooking(booking.getGuest().getId(), "VIEW_BOOKING_SUCCESS",
                    booking.getId(), Map.of("bookingCode", booking.getBookingCode()));

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Error getting booking: {}", e.getMessage());
            auditLogService.logBooking(null, "VIEW_BOOKING_ERROR", bookingId,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ List Bookings ------------------
    @Override
    public ResponseEntity<List<BookingResponseDTO>> listBookingsForGuest(Long guestId, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            if (!isAdmin() && (currentUserId == null || !currentUserId.equals(guest.getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<BookingResponseDTO> bookings = bookingRepository.findByGuestId(guestId).stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            auditLogService.logBooking(guestId, "LIST_BOOKINGS_SUCCESS", null,
                    Map.of("count", bookings.size()));
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error listing bookings: {}", e.getMessage());
            auditLogService.logBooking(guestId, "LIST_BOOKINGS_ERROR", null,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ------------------ Add Services to Booking ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> addServicesToBooking(Long bookingId, List<Long> serviceIds, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin() && (currentUserId == null || !currentUserId.equals(booking.getGuest().getId()))) {
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
            auditLogService.logBooking(null, "ADD_SERVICES_ERROR", bookingId,
                    Map.of("error", e.getMessage(), "serviceIds", serviceIds));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> cancelBooking(Long bookingId, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Update booking status
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Make room available again
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(true);
                roomRepository.save(room);
            }

            // Delete invoice from Cloudinary and remove record from DB
            Invoice invoice = booking.getInvoice();
            if (invoice != null) {
                if (invoice.getInvoiceUrl() != null) {
                    String publicId = cloudinaryService.extractPublicIdFromUrl(invoice.getInvoiceUrl());
                    cloudinaryService.deleteFile(publicId);
                }
                if (invoice.getInvoiceUrlMedium() != null) {
                    String publicIdMedium = cloudinaryService.extractPublicIdFromUrl(invoice.getInvoiceUrlMedium());
                    cloudinaryService.deleteFile(publicIdMedium);
                }
                if (invoice.getInvoiceUrlThumbnail() != null) {
                    String publicIdThumb = cloudinaryService.extractPublicIdFromUrl(invoice.getInvoiceUrlThumbnail());
                    cloudinaryService.deleteFile(publicIdThumb);
                }

                invoiceRepository.delete(invoice);
                booking.setInvoice(null); // ensure booking reflects deleted invoice
            }

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "CANCEL_BOOKING_SUCCESS",
                    booking.getId(),
                    Map.of("roomId", room != null ? room.getId() : null)
            );

            // Return updated BookingResponseDTO with invoiceExists = false
            return ResponseEntity.ok(toBookingResponse(booking));

        } catch (Exception e) {
            log.error("Cancel booking error: {}", e.getMessage(), e);
            auditLogService.logBooking(null, "CANCEL_BOOKING_ERROR", bookingId,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<BookingResponseDTO> findByBookingCode(String code, Long currentUserId) {
        try {
            Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(code);
            if (bookingOpt.isEmpty()) return ResponseEntity.ok(null);

            Booking booking = bookingOpt.get();

            // Permission check: only admin/staff or the guest who owns the booking
            if (!isAdmin() && (currentUserId == null || !currentUserId.equals(booking.getGuest().getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logBooking(
                    booking.getGuest().getId(),
                    "FIND_BOOKING_BY_CODE_SUCCESS",
                    booking.getId(),
                    Map.of("bookingCode", code)
            );

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Error finding booking by code: {}", e.getMessage(), e);
            auditLogService.logBooking(
                    null,
                    "FIND_BOOKING_BY_CODE_ERROR",
                    null,
                    Map.of("bookingCode", code, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Check-In ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> checkIn(Long bookingId, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            booking.setStatus(BookingStatus.CHECKED_IN);
            bookingRepository.save(booking);

            auditLogService.logBooking(booking.getGuest().getId(), "CHECK_IN_SUCCESS",
                    booking.getId(), Map.of("bookingCode", booking.getBookingCode()));

            return ResponseEntity.ok(toBookingResponse(booking));
        } catch (Exception e) {
            log.error("Check-in error: {}", e.getMessage());
            auditLogService.logBooking(null, "CHECK_IN_ERROR", bookingId,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------ Check-Out ------------------
    @Override
    @Transactional
    public ResponseEntity<BookingResponseDTO> checkOut(Long bookingId, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            booking.setStatus(BookingStatus.CHECKED_OUT);
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
            auditLogService.logBooking(null, "CHECK_OUT_ERROR", bookingId,
                    Map.of("error", e.getMessage()));
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
            auditLogService.logBooking(null, "AUTO_RELEASE_ROOM_ERROR", null,
                    Map.of("error", e.getMessage()));
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

            auditLogService.logBooking(null, "FIND_AVAILABLE_ROOMS_SUCCESS", null,
                    Map.of("count", availableRooms.size()));

            return ResponseEntity.ok(availableRooms);
        } catch (Exception e) {
            log.error("Error finding rooms: {}", e.getMessage());
            auditLogService.logBooking(null, "FIND_AVAILABLE_ROOMS_ERROR", null,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ------------------ Get All Bookings ------------------
    @Override
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings(Long currentUserId) {
        try {
            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<BookingResponseDTO> bookings = bookingRepository.findAll().stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error getting all bookings: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<BookingResponseDTO>> filterBookings(LocalDate startDate, LocalDate endDate, String status, Long currentUserId) {
        try {
            List<Booking> bookings;

            // Admin/staff: can filter all bookings
            if (isAdmin()) {
                if (status != null && !status.isEmpty()) {
                    bookings = bookingRepository.findByCheckInDateBetweenAndStatus(startDate, endDate, BookingStatus.valueOf(status));
                } else {
                    bookings = bookingRepository.findByCheckInDateBetween(startDate, endDate);
                }
            } else {
                // Guests: only their own bookings
                if (currentUserId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                if (status != null && !status.isEmpty()) {
                    bookings = bookingRepository.findByGuestIdAndCheckInDateBetweenAndStatus(
                            currentUserId, startDate, endDate, BookingStatus.valueOf(status)
                    );
                } else {
                    bookings = bookingRepository.findByGuestIdAndCheckInDateBetween(
                            currentUserId, startDate, endDate
                    );
                }
            }

            List<BookingResponseDTO> result = bookings.stream()
                    .map(this::toBookingResponse)
                    .collect(Collectors.toList());

            auditLogService.logBooking(
                    currentUserId,
                    "FILTER_BOOKINGS_SUCCESS",
                    null,
                    Map.of("count", result.size(), "startDate", startDate, "endDate", endDate, "status", status)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error filtering bookings: {}", e.getMessage(), e);
            auditLogService.logBooking(
                    currentUserId,
                    "FILTER_BOOKINGS_ERROR",
                    null,
                    Map.of("startDate", startDate, "endDate", endDate, "status", status, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ------------------ Mapping Helpers ------------------
    private BookingResponseDTO toBookingResponse(Booking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .status(booking.getStatus())
                .room(toRoomResponse(booking.getRoom()))
                .guest(toGuestResponse(booking.getGuest()))
                .services(booking.getServices() != null
                        ? booking.getServices().stream().map(this::toServiceResponse).toList()
                        : Collections.emptyList())
                .invoiceExists(booking.getInvoice() != null)
                .build();
    }

    private RoomResponseDTO toRoomResponse(Room room) {
        if (room == null) return null;
        return RoomResponseDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .pricePerNight(room.getPricePerNight())
                .type(room.getType())
                .available(room.isAvailable())
                .build();
    }

    private GuestResponseDTO toGuestResponse(Guest guest) {
        if (guest == null) return null;
        return GuestResponseDTO.builder()
                .id(guest.getId())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .phoneNumber(guest.getPhoneNumber())
                .idNumber(guest.getIdNumber())
                .build();
    }

    private ServiceResponseDTO toServiceResponse(com.justine.model.Service service) {
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .build();
    }

    private PaymentResponseDTO toPaymentResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaymentDate())
                .build();
    }

    private InvoiceResponseDTO toInvoiceResponse(Invoice invoice) {
        if (invoice == null) return null;
        return InvoiceResponseDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .issuedDate(invoice.getIssuedDate())
                .totalAmount(invoice.getTotalAmount())
                .paid(invoice.isPaid())
                .invoiceUrl(invoice.getInvoiceUrl())
                .invoiceUrlMedium(invoice.getInvoiceUrlMedium())
                .invoiceUrlThumbnail(invoice.getInvoiceUrlThumbnail())
                .generatedAt(invoice.getGeneratedAt())
                .build();
    }

}
