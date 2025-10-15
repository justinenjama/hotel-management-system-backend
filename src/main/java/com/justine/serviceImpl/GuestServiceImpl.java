package com.justine.serviceImpl;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.model.Guest;
import com.justine.repository.BookingRepository;
import com.justine.repository.GuestRepository;
import com.justine.repository.RestaurantOrderRepository;
import com.justine.service.AuditLogService;
import com.justine.service.GuestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final RestaurantOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public GuestServiceImpl(GuestRepository guestRepository, BookingRepository bookingRepository, RestaurantOrderRepository orderRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    private boolean isAdmin(String email) {
        return guestRepository.findByEmail(email)
                .map(g -> "ADMIN".equalsIgnoreCase(g.getRole()))
                .orElse(false);
    }

    @Override
    public ResponseEntity<GuestResponseDTO> createGuest(GuestDTO dto) {
        try {
            if (guestRepository.existsByEmail(dto.getEmail())) {
                log.warn("Guest creation failed: email {} already exists", dto.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            Guest guest = Guest.builder()
                    .fullName(dto.getFullName())
                    .email(dto.getEmail())
                    .phoneNumber(dto.getPhoneNumber())
                    .idNumber(dto.getIdNumber())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .role("USER")
                    .gender(dto.getGender())
                    .build();

            guestRepository.save(guest);
            log.info("Guest created successfully: {}", guest.getEmail());

            auditLogService.logGuest(guest.getId(), "CREATE_GUEST", guest.getId(),
                    Map.of("email", guest.getEmail(), "name", guest.getFullName()));

            return ResponseEntity.status(HttpStatus.CREATED).body(toGuestResponse(guest));
        } catch (Exception e) {
            log.error("Error creating guest: {}", e.getMessage(), e);
            auditLogService.logGuest(null, "CREATE_GUEST_ERROR", null,
                    Map.of("error", e.getMessage(), "email", dto.getEmail()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<GuestResponseDTO> updateGuest(Long guestId, GuestDTO dto, String currentUserEmail) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin(currentUserEmail) && !guest.getEmail().equals(currentUserEmail)) {
                log.warn("Unauthorized guest update attempt by {}", currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            guest.setFullName(dto.getFullName());
            guest.setPhoneNumber(dto.getPhoneNumber());
            guest.setIdNumber(dto.getIdNumber());
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                guest.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            guestRepository.save(guest);
            log.info("Guest {} updated successfully by {}", guestId, currentUserEmail);

            auditLogService.logGuest(guestId, "UPDATE_GUEST", guestId,
                    Map.of("updatedBy", currentUserEmail, "newName", dto.getFullName()));

            return ResponseEntity.ok(toGuestResponse(guest));
        } catch (NoSuchElementException e) {
            log.warn("Guest not found: {}", guestId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating guest {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "UPDATE_GUEST_ERROR", guestId,
                    Map.of("error", e.getMessage(), "updatedBy", currentUserEmail));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<GuestResponseDTO> getGuestById(Long id, String currentUserEmail) {
        try {
            Guest guest = guestRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin(currentUserEmail) && !guest.getEmail().equals(currentUserEmail)) {
                log.warn("Unauthorized access to guest {} by {}", id, currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logGuest(id, "VIEW_GUEST", id,
                    Map.of("viewer", currentUserEmail));

            log.info("Guest {} viewed by {}", id, currentUserEmail);
            return ResponseEntity.ok(toGuestResponse(guest));
        } catch (NoSuchElementException e) {
            log.warn("Guest not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching guest {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<GuestResponseDTO>> getAllGuests(String currentUserEmail) {
        try {
            if (!isAdmin(currentUserEmail)) {
                log.warn("Unauthorized guest list access attempt by {}", currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> guests = guestRepository.findAll()
                    .stream()
                    .map(this::toGuestResponse)
                    .collect(Collectors.toList());

            log.info("Guest list retrieved successfully by {} ({} records)", currentUserEmail, guests.size());
            auditLogService.logGuest(null, "LIST_GUESTS", null,
                    Map.of("requestedBy", currentUserEmail, "count", guests.size()));

            return ResponseEntity.ok(guests);
        } catch (Exception e) {
            log.error("Error listing guests: {}", e.getMessage(), e);
            auditLogService.logGuest(null, "LIST_GUESTS_ERROR", null,
                    Map.of("requestedBy", currentUserEmail, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteGuest(Long id, String currentUserEmail) {
        try {
            if (!isAdmin(currentUserEmail)) {
                log.warn("Unauthorized delete attempt by {}", currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            guestRepository.deleteById(id);
            log.info("Guest {} deleted successfully by {}", id, currentUserEmail);

            auditLogService.logGuest(id, "DELETE_GUEST", id,
                    Map.of("deletedBy", currentUserEmail));

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting guest {}: {}", id, e.getMessage(), e);
            auditLogService.logGuest(id, "DELETE_GUEST_ERROR", id,
                    Map.of("deletedBy", currentUserEmail, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<GuestResponseDTO>> getGuestBookings(Long guestId, String currentUserEmail) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin(currentUserEmail) && !guest.getEmail().equals(currentUserEmail)) {
                log.warn("Unauthorized access to bookings of guest {} by {}", guestId, currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> response = bookingRepository.findByGuestId(guestId)
                    .stream()
                    .map(b -> toGuestResponse(b.getGuest()))
                    .collect(Collectors.toList());

            log.info("Guest {} bookings fetched successfully by {}", guestId, currentUserEmail);
            auditLogService.logGuest(guestId, "VIEW_BOOKINGS", guestId,
                    Map.of("viewer", currentUserEmail, "bookingCount", response.size()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching guest bookings for {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "VIEW_BOOKINGS_ERROR", guestId,
                    Map.of("viewer", currentUserEmail, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<GuestResponseDTO>> getGuestOrders(Long guestId, String currentUserEmail) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin(currentUserEmail) && !guest.getEmail().equals(currentUserEmail)) {
                log.warn("Unauthorized access to orders of guest {} by {}", guestId, currentUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> response = orderRepository.findByGuestId(guestId)
                    .stream()
                    .map(o -> toGuestResponse(o.getGuest()))
                    .collect(Collectors.toList());

            log.info("Guest {} orders fetched successfully by {}", guestId, currentUserEmail);
            auditLogService.logGuest(guestId, "VIEW_ORDERS", guestId,
                    Map.of("viewer", currentUserEmail, "orderCount", response.size()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching guest orders for {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "VIEW_ORDERS_ERROR", guestId,
                    Map.of("viewer", currentUserEmail, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    private GuestResponseDTO toGuestResponse(Guest guest) {
        if (guest == null) return null;
        return GuestResponseDTO.builder()
                .id(guest.getId())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .phoneNumber(guest.getPhoneNumber())
                .idNumber(guest.getIdNumber())
                .role(guest.getRole())
                .gender(guest.getGender())
                .build();
    }
}
