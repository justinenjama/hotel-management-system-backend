package com.justine.serviceImpl;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.model.Guest;
import com.justine.repository.BookingRepository;
import com.justine.repository.GuestRepository;
import com.justine.repository.RestaurantOrderRepository;
import com.justine.service.AuditLogService;
import com.justine.service.GuestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<GuestResponseDTO> updateGuest(Long guestId, GuestDTO dto, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            boolean admin = isAdmin();
            boolean selfUpdate = guest.getId().equals(currentUserId);

            if (!admin && !selfUpdate) {
                log.warn("Unauthorized guest update attempt by {}", currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body((GuestResponseDTO) Map.of("error", "You are not allowed to update this guest"));
            }

            // Update provided fields
            if (dto.getFullName() != null) guest.setFullName(dto.getFullName());
            if (dto.getPhoneNumber() != null) guest.setPhoneNumber(dto.getPhoneNumber());
            if (dto.getIdNumber() != null) guest.setIdNumber(dto.getIdNumber());
            if (dto.getGender() != null) guest.setGender(dto.getGender());
            if (dto.getEmail() != null) guest.setEmail(dto.getEmail());

            // Password handling
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                if (admin) {
                    // Admin can directly set password
                    guest.setPassword(passwordEncoder.encode(dto.getPassword()));
                } else if (selfUpdate) {
                    // Guest must provide old password to change
                    if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body((GuestResponseDTO) Map.of("error", "Old password is required"));
                    }
                    if (!passwordEncoder.matches(dto.getOldPassword(), guest.getPassword())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body((GuestResponseDTO) Map.of("error", "Old password is incorrect"));
                    }
                    guest.setPassword(passwordEncoder.encode(dto.getPassword()));
                }
            }

            guestRepository.save(guest);
            log.info("Guest {} updated successfully by {}", guestId, currentUserId);

            auditLogService.logGuest(guestId, "UPDATE_GUEST", guestId,
                    Map.of("updatedBy", currentUserId, "newName", guest.getFullName()));

            return ResponseEntity.ok(toGuestResponse(guest));

        } catch (NoSuchElementException e) {
            log.warn("Guest not found: {}", guestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body((GuestResponseDTO) Map.of("error", "Guest not found"));
        } catch (Exception e) {
            log.error("Error updating guest {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "UPDATE_GUEST_ERROR", guestId,
                    Map.of("error", e.getMessage(), "updatedBy", currentUserId));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body((GuestResponseDTO) Map.of("error", "Failed to update guest"));
        }
    }

    @Override
    public ResponseEntity<GuestResponseDTO> getGuestById(Long id, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin() && !guest.getId().equals(currentUserId)) {
                log.warn("Unauthorized access to guest {} by {}", id, currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logGuest(id, "VIEW_GUEST", id, Map.of("viewer", currentUserId));

            log.info("Guest {} viewed by {}", id, currentUserId);
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
    public ResponseEntity<List<GuestResponseDTO>> getAllGuests(Long currentUserId) {
        try {
            if (!isAdmin()) {
                log.warn("Unauthorized guest list access attempt by {}", currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> guests = guestRepository.findAll()
                    .stream()
                    .map(this::toGuestResponse)
                    .collect(Collectors.toList());

            log.info("Guest list retrieved successfully by {} ({} records)", currentUserId, guests.size());
            auditLogService.logGuest(null, "LIST_GUESTS", null,
                    Map.of("requestedBy", currentUserId, "count", guests.size()));

            return ResponseEntity.ok(guests);
        } catch (Exception e) {
            log.error("Error listing guests: {}", e.getMessage(), e);
            auditLogService.logGuest(null, "LIST_GUESTS_ERROR", null,
                    Map.of("requestedBy", currentUserId, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteGuest(Long id, Long currentUserId) {
        try {
            if (!isAdmin()) {
                log.warn("Unauthorized delete attempt by {}", currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            guestRepository.deleteById(id);
            log.info("Guest {} deleted successfully by {}", id, currentUserId);

            auditLogService.logGuest(id, "DELETE_GUEST", id, Map.of("deletedBy", currentUserId));

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting guest {}: {}", id, e.getMessage(), e);
            auditLogService.logGuest(id, "DELETE_GUEST_ERROR", id,
                    Map.of("deletedBy", currentUserId, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<GuestResponseDTO>> getGuestBookings(Long guestId, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin() && !guest.getId().equals(currentUserId)) {
                log.warn("Unauthorized access to bookings of guest {} by {}", guestId, currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> response = bookingRepository.findByGuestId(guestId)
                    .stream()
                    .map(b -> toGuestResponse(b.getGuest()))
                    .collect(Collectors.toList());

            log.info("Guest {} bookings fetched successfully by {}", guestId, currentUserId);
            auditLogService.logGuest(guestId, "VIEW_BOOKINGS", guestId,
                    Map.of("viewer", currentUserId, "bookingCount", response.size()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching guest bookings for {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "VIEW_BOOKINGS_ERROR", guestId,
                    Map.of("viewer", currentUserId, "error", e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<GuestResponseDTO>> getGuestOrders(Long guestId, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new NoSuchElementException("Guest not found"));

            if (!isAdmin() && !guest.getId().equals(currentUserId)) {
                log.warn("Unauthorized access to orders of guest {} by {}", guestId, currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<GuestResponseDTO> response = orderRepository.findByGuestId(guestId)
                    .stream()
                    .map(o -> toGuestResponse(o.getGuest()))
                    .collect(Collectors.toList());

            log.info("Guest {} orders fetched successfully by {}", guestId, currentUserId);
            auditLogService.logGuest(guestId, "VIEW_ORDERS", guestId,
                    Map.of("viewer", currentUserId, "orderCount", response.size()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching guest orders for {}: {}", guestId, e.getMessage(), e);
            auditLogService.logGuest(guestId, "VIEW_ORDERS_ERROR", guestId,
                    Map.of("viewer", currentUserId, "error", e.getMessage()));
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
                .createdAt(guest.getCreatedAt())
                .build();
    }
}
