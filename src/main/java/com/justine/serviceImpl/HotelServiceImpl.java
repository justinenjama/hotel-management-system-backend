package com.justine.serviceImpl;

import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.*;
import com.justine.model.*;
import com.justine.repository.*;
import com.justine.service.AuditLogService;
import com.justine.service.HotelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;

    public HotelServiceImpl(HotelRepository hotelRepository, RoomRepository roomRepository, AuditLogService auditLogService) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.auditLogService = auditLogService;
    }

    /** Ensure only ADMIN can perform hotel operations */
    private void checkAdminAccess() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null ||
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new SecurityException("Access denied: Admin privileges required");
        }
    }

    private Long getActorIdFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Staff staff) {
            return staff.getId();
        }
        return null; // fallback for system calls
    }

    // -------------------- CREATE HOTEL --------------------
    @Override
    public HotelResponseDTO createHotel(HotelRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = Hotel.builder()
                    .name(dto.getName())
                    .location(dto.getLocation())
                    .contactNumber(dto.getContactNumber())
                    .email(dto.getEmail())
                    .build();

            Hotel saved = hotelRepository.save(hotel);

            auditLogService.logHotel(actorId, "CREATE_HOTEL", saved.getId(),
                    Map.of("name", saved.getName(), "location", saved.getLocation()));

            log.info("Hotel created successfully by Admin (ID: {}): {}", actorId, saved.getName());
            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Error creating hotel by Admin (ID: {}): {}", actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "CREATE_HOTEL_ERROR", null,
                    Map.of("error", e.getMessage(), "request", dto));
            throw e;
        }
    }

    // -------------------- UPDATE HOTEL --------------------
    @Override
    public HotelResponseDTO updateHotel(Long id, HotelRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            hotel.setName(dto.getName());
            hotel.setLocation(dto.getLocation());
            hotel.setContactNumber(dto.getContactNumber());
            hotel.setEmail(dto.getEmail());

            Hotel updated = hotelRepository.save(hotel);

            auditLogService.logHotel(actorId, "UPDATE_HOTEL", updated.getId(),
                    Map.of("updatedFields", dto));

            log.info("Hotel updated successfully by Admin (ID: {}): {}", actorId, updated.getName());
            return mapToResponse(updated);
        } catch (Exception e) {
            log.error("Error updating hotel {} by Admin (ID: {}): {}", id, actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "UPDATE_HOTEL_ERROR", id,
                    Map.of("error", e.getMessage(), "hotelId", id, "request", dto));
            throw e;
        }
    }

    // -------------------- DELETE HOTEL --------------------
    @Override
    public void deleteHotel(Long id) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            hotelRepository.deleteById(id);
            auditLogService.logHotel(actorId, "DELETE_HOTEL", id, Map.of("deletedId", id));
            log.info("Hotel deleted by Admin (ID: {}), Hotel ID: {}", actorId, id);
        } catch (Exception e) {
            log.error("Error deleting hotel {} by Admin (ID: {}): {}", id, actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "DELETE_HOTEL_ERROR", id,
                    Map.of("error", e.getMessage(), "hotelId", id));
            throw e;
        }
    }

    // -------------------- GET HOTEL BY ID --------------------
    @Override
    public HotelResponseDTO getHotelById(Long id) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            auditLogService.logHotel(actorId, "VIEW_HOTEL", id,
                    Map.of("hotelName", hotel.getName()));

            log.info("Hotel {} viewed by Admin (ID: {})", id, actorId);
            return mapToResponse(hotel);
        } catch (Exception e) {
            log.error("Error fetching hotel {} by Admin (ID: {}): {}", id, actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "VIEW_HOTEL_ERROR", id,
                    Map.of("error", e.getMessage(), "hotelId", id));
            throw e;
        }
    }

    // -------------------- LIST ALL HOTELS --------------------
    @Override
    public List<HotelResponseDTO> getAllHotels() {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            List<Hotel> hotels = hotelRepository.findAll();
            List<HotelResponseDTO> responses = hotels.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            auditLogService.logHotel(actorId, "LIST_HOTELS", null,
                    Map.of("count", responses.size()));

            log.info("Admin (ID: {}) fetched all hotels ({} records)", actorId, responses.size());
            return responses;
        } catch (Exception e) {
            log.error("Error fetching all hotels by Admin (ID: {}): {}", actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "LIST_HOTELS_ERROR", null,
                    Map.of("error", e.getMessage()));
            return Collections.emptyList();
        }
    }

    // -------------------- ADD ROOM --------------------
    @Override
    public RoomResponseDTO addRoomToHotel(RoomRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(dto.getHotelId())
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            Room room = Room.builder()
                    .roomNumber(dto.getRoomNumber())
                    .type(dto.getType())
                    .pricePerNight(dto.getPricePerNight())
                    .available(dto.isAvailable())
                    .hotel(hotel)
                    .build();

            Room saved = roomRepository.save(room);

            auditLogService.logHotel(actorId, "ADD_ROOM", hotel.getId(),
                    Map.of("roomNumber", saved.getRoomNumber(), "type", saved.getType()));

            log.info("Room added by Admin (ID: {}) to Hotel {}: {}", actorId, hotel.getName(), saved.getRoomNumber());
            return mapRoomToResponse(saved);
        } catch (Exception e) {
            log.error("Error adding room by Admin (ID: {}): {}", actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "ADD_ROOM_ERROR", dto.getHotelId(),
                    Map.of("error", e.getMessage(), "request", dto));
            throw e;
        }
    }

    // -------------------- UPDATE ROOM --------------------
    @Override
    public RoomResponseDTO updateRoom(Long roomId, RoomRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));

            room.setRoomNumber(dto.getRoomNumber());
            room.setType(dto.getType());
            room.setPricePerNight(dto.getPricePerNight());
            room.setAvailable(dto.isAvailable());

            Room updated = roomRepository.save(room);

            auditLogService.logHotel(actorId, "UPDATE_ROOM",
                    room.getHotel() != null ? room.getHotel().getId() : null,
                    Map.of("roomId", roomId, "updatedFields", dto));

            log.info("Room updated by Admin (ID: {}): {}", actorId, room.getRoomNumber());
            return mapRoomToResponse(updated);
        } catch (Exception e) {
            log.error("Error updating room {} by Admin (ID: {}): {}", roomId, actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "UPDATE_ROOM_ERROR", roomId,
                    Map.of("error", e.getMessage(), "roomId", roomId, "request", dto));
            throw e;
        }
    }

    // -------------------- DELETE ROOM --------------------
    @Override
    public void deleteRoom(Long roomId) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            roomRepository.deleteById(roomId);
            auditLogService.logHotel(actorId, "DELETE_ROOM", null, Map.of("roomId", roomId));
            log.info("Room deleted by Admin (ID: {}), Room ID: {}", actorId, roomId);
        } catch (Exception e) {
            log.error("Error deleting room {} by Admin (ID: {}): {}", roomId, actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "DELETE_ROOM_ERROR", null,
                    Map.of("error", e.getMessage(), "roomId", roomId));
            throw e;
        }
    }

    // -------------------- Mapping Helpers --------------------
    private HotelResponseDTO mapToResponse(Hotel hotel) {
        return HotelResponseDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .location(hotel.getLocation())
                .contactNumber(hotel.getContactNumber())
                .email(hotel.getEmail())
                .rooms(hotel.getRooms() != null
                        ? hotel.getRooms().stream().map(this::mapRoomToResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .staffMembers(hotel.getStaffMembers() != null
                        ? hotel.getStaffMembers().stream().map(this::mapStaffToResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    private RoomResponseDTO mapRoomToResponse(Room room) {
        return RoomResponseDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .pricePerNight(room.getPricePerNight())
                .available(room.isAvailable())
                .build();
    }

    private StaffResponseDTO mapStaffToResponse(Staff staff) {
        return StaffResponseDTO.builder()
                .id(staff.getId())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .role(staff.getRole())
                .accessToken(staff.getAccessToken())
                .refreshToken(staff.getRefreshToken())
                .build();
    }
}
