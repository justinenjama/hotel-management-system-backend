package com.justine.serviceImpl;

import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.StaffResponseDTO;
import com.justine.enums.StaffRole;
import com.justine.model.Hotel;
import com.justine.model.Staff;
import com.justine.repository.HotelRepository;
import com.justine.repository.StaffRepository;
import com.justine.service.AuditLogService;
import com.justine.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public StaffServiceImpl(StaffRepository staffRepository, HotelRepository hotelRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.staffRepository = staffRepository;
        this.hotelRepository = hotelRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    private boolean isAdmin(Long currentUserId) {
        return staffRepository.findById(currentUserId)
                .map(staff -> staff.getRole() == StaffRole.ADMIN)
                .orElse(false);
    }

    private Long getActorId(Long currentUserId) {
        return currentUserId;
    }

    // ============ ADD STAFF ============
    @Override
    public ResponseEntity<StaffResponseDTO> addStaff(StaffRequestDTO dto, Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_ADD_STAFF", null,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (staffRepository.existsByEmail(dto.getEmail())) {
                auditLogService.logStaff(actorId, "STAFF_ALREADY_EXISTS", null,
                        Map.of("email", dto.getEmail()));
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            Hotel hotel = null;
            if (dto.getHotelId() != null) {
                hotel = hotelRepository.findById(dto.getHotelId())
                        .orElseThrow(() -> new NoSuchElementException("Hotel not found"));
            }

            Staff staff = Staff.builder()
                    .fullName(dto.getFullName())
                    .email(dto.getEmail())
                    .phoneNumber(dto.getPhoneNumber())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .role(dto.getRole())
                    .hotel(hotel)
                    .gender(dto.getGender())
                    .build();

            staffRepository.save(staff);

            auditLogService.logStaff(actorId, "ADD_STAFF_SUCCESS", staff.getId(),
                    Map.of("userId", currentUserId, "email", dto.getEmail()));

            return ResponseEntity.status(HttpStatus.CREATED).body(toStaffResponse(staff));

        } catch (Exception e) {
            auditLogService.logStaff(actorId, "ADD_STAFF_FAILED", null,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ UPDATE STAFF ============
    @Override
    public ResponseEntity<StaffResponseDTO> updateStaff(Long staffId, StaffRequestDTO dto, Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new NoSuchElementException("Staff not found"));

            if (!isAdmin(currentUserId) && !staff.getId().equals(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_UPDATE_STAFF", staffId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            staff.setFullName(dto.getFullName());
            staff.setPhoneNumber(dto.getPhoneNumber());
            if (dto.getPassword() != null) {
                staff.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            if (dto.getRole() != null && isAdmin(currentUserId)) {
                staff.setRole(dto.getRole());
            }

            staffRepository.save(staff);

            auditLogService.logStaff(actorId, "UPDATE_STAFF_SUCCESS", staffId,
                    Map.of("userId", currentUserId, "role", staff.getRole().toString()));

            return ResponseEntity.ok(toStaffResponse(staff));

        } catch (NoSuchElementException e) {
            auditLogService.logStaff(actorId, "UPDATE_STAFF_NOT_FOUND", staffId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            auditLogService.logStaff(actorId, "UPDATE_STAFF_FAILED", staffId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ DELETE STAFF ============
    @Override
    public ResponseEntity<Void> deleteStaff(Long staffId, Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_DELETE_STAFF", staffId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            staffRepository.deleteById(staffId);

            auditLogService.logStaff(actorId, "DELETE_STAFF_SUCCESS", staffId,
                    Map.of("userId", currentUserId));

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            auditLogService.logStaff(actorId, "DELETE_STAFF_FAILED", staffId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ ASSIGN STAFF TO HOTEL ============
    @Override
    public ResponseEntity<StaffResponseDTO> assignStaffToHotel(Long staffId, Long hotelId, Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_ASSIGN_STAFF", staffId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new NoSuchElementException("Staff not found"));
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new NoSuchElementException("Hotel not found"));

            staff.setHotel(hotel);
            staffRepository.save(staff);

            auditLogService.logStaff(actorId, "ASSIGN_STAFF_SUCCESS", staffId,
                    Map.of("hotelId", hotelId, "userId", currentUserId));

            return ResponseEntity.ok(toStaffResponse(staff));

        } catch (Exception e) {
            auditLogService.logStaff(actorId, "ASSIGN_STAFF_FAILED", staffId,
                    Map.of("hotelId", hotelId, "userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ GET STAFF BY ID ============
    @Override
    public ResponseEntity<StaffResponseDTO> getStaffById(Long staffId, Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new NoSuchElementException("Staff not found"));

            if (!isAdmin(currentUserId) && !staff.getId().equals(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_GET_STAFF", staffId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logStaff(actorId, "GET_STAFF_SUCCESS", staffId,
                    Map.of("userId", currentUserId));

            return ResponseEntity.ok(toStaffResponse(staff));

        } catch (NoSuchElementException e) {
            auditLogService.logStaff(actorId, "GET_STAFF_NOT_FOUND", staffId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            auditLogService.logStaff(actorId, "GET_STAFF_FAILED", staffId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ GET ALL STAFF ============
    @Override
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff(Long currentUserId) {
        Long actorId = getActorId(currentUserId);
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logStaff(actorId, "UNAUTHORIZED_GET_ALL_STAFF", null,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<StaffResponseDTO> staffList = staffRepository.findAll()
                    .stream()
                    .map(this::toStaffResponse)
                    .collect(Collectors.toList());

            auditLogService.logStaff(actorId, "GET_ALL_STAFF_SUCCESS", null,
                    Map.of("count", staffList.size(), "userId", currentUserId));

            return ResponseEntity.ok(staffList);

        } catch (Exception e) {
            auditLogService.logStaff(actorId, "GET_ALL_STAFF_FAILED", null,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ MAPPER ============
    private StaffResponseDTO toStaffResponse(Staff staff) {
        HotelResponseDTO hotelDTO = null;
        if (staff.getHotel() != null) {
            hotelDTO = HotelResponseDTO.builder()
                    .id(staff.getHotel().getId())
                    .name(staff.getHotel().getName())
                    .location(staff.getHotel().getLocation())
                    .contactNumber(staff.getHotel().getContactNumber())
                    .email(staff.getHotel().getEmail())
                    .build();
        }

        return StaffResponseDTO.builder()
                .id(staff.getId())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .role(staff.getRole())
                .gender(staff.getGender())
                .hotel(hotelDTO)
                .build();
    }
}
