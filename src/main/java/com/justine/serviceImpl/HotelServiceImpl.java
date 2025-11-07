package com.justine.serviceImpl;

import com.justine.dtos.request.CreateServiceRequest;
import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import com.justine.dtos.response.StaffResponseDTO;
import com.justine.enums.BookingStatus;
import com.justine.model.Booking;
import com.justine.model.Hotel;
import com.justine.model.Room;
import com.justine.model.Staff;
import com.justine.repository.HotelRepository;
import com.justine.repository.RoomRepository;
import com.justine.repository.ServiceRepository;
import com.justine.service.AuditLogService;
import com.justine.service.HotelService;
import com.justine.utils.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;
    private final CloudinaryService cloudinaryService;

    public HotelServiceImpl(
            HotelRepository hotelRepository, ServiceRepository serviceRepository,
            RoomRepository roomRepository,
            AuditLogService auditLogService,
            CloudinaryService cloudinaryService) {
        this.hotelRepository = hotelRepository;
        this.serviceRepository = serviceRepository;
        this.roomRepository = roomRepository;
        this.auditLogService = auditLogService;
        this.cloudinaryService = cloudinaryService;
    }

    /* ====================== ACCESS CONTROL ====================== */
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
        return null;
    }

    private void safeDeleteImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            String publicId = cloudinaryService.extractPublicIdFromUrl(imageUrl);
            if (publicId != null) cloudinaryService.deleteFile(publicId);
        }
    }

    /* ====================== HOTEL CRUD ====================== */
    @Override
    @Transactional
    public HotelResponseDTO createHotel(HotelRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Map<String, String> imageUrls = null;
            if (dto.getHotelImageFile() != null && !dto.getHotelImageFile().isEmpty()) {
                imageUrls = cloudinaryService.uploadFileWithEagerSizes(dto.getHotelImageFile(), "hotels");
            }

            Hotel hotel = Hotel.builder()
                    .name(dto.getName())
                    .location(dto.getLocation())
                    .contactNumber(dto.getContactNumber())
                    .email(dto.getEmail())
                    .hotelImageUrl(imageUrls != null ? imageUrls.get("large") : null)
                    .build();

            Hotel saved = hotelRepository.save(hotel);

            auditLogService.logHotel(actorId, "CREATE_HOTEL", saved.getId(),
                    Map.of("name", saved.getName(), "imageUrls", imageUrls));

            log.info("Hotel created by Admin (ID: {}): {}", actorId, saved.getName());
            return mapToResponse(saved);

        } catch (Exception e) {
            log.error("Error creating hotel by Admin (ID: {}): {}", actorId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "CREATE_HOTEL_ERROR", null,
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    @Override
    @Transactional
    public HotelResponseDTO updateHotel(Long id, HotelRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            if (dto.getHotelImageFile() != null && !dto.getHotelImageFile().isEmpty()) {
                safeDeleteImage(hotel.getHotelImageUrl());
                Map<String, String> imageUrls = cloudinaryService.uploadFileWithEagerSizes(dto.getHotelImageFile(), "hotels");
                hotel.setHotelImageUrl(imageUrls.get("large"));
            }

            hotel.setName(dto.getName());
            hotel.setLocation(dto.getLocation());
            hotel.setContactNumber(dto.getContactNumber());
            hotel.setEmail(dto.getEmail());

            Hotel updated = hotelRepository.save(hotel);

            auditLogService.logHotel(actorId, "UPDATE_HOTEL", id,
                    Map.of("updatedFields", dto));
            log.info("Hotel {} updated by Admin (ID: {})", id, actorId);

            return mapToResponse(updated);

        } catch (Exception e) {
            log.error("Error updating hotel {}: {}", id, e.getMessage(), e);
            auditLogService.logHotel(actorId, "UPDATE_HOTEL_ERROR", id,
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteHotel(Long id) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            safeDeleteImage(hotel.getHotelImageUrl());

            List<Room> rooms = roomRepository.findByHotelId(id);
            rooms.forEach(room -> safeDeleteImage(room.getRoomImageUrl()));

            hotelRepository.delete(hotel);

            auditLogService.logHotel(actorId, "DELETE_HOTEL", id, Map.of("deletedId", id));
            log.info("Hotel {} deleted (with images) by Admin (ID: {})", id, actorId);

        } catch (Exception e) {
            log.error("Error deleting hotel {}: {}", id, e.getMessage(), e);
            auditLogService.logHotel(actorId, "DELETE_HOTEL_ERROR", id,
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    /* ====================== ROOM CRUD ====================== */
    @Override
    @Transactional
    public RoomResponseDTO addRoomToHotel(RoomRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Hotel hotel = hotelRepository.findById(dto.getHotelId())
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            Map<String, String> imageUrls = null;
            if (dto.getRoomImageFile() != null && !dto.getRoomImageFile().isEmpty()) {
                imageUrls = cloudinaryService.uploadFileWithEagerSizes(dto.getRoomImageFile(), "rooms");
            }

            Room room = Room.builder()
                    .roomNumber(dto.getRoomNumber())
                    .type(dto.getType())
                    .pricePerNight(dto.getPricePerNight())
                    .available(dto.isAvailable())
                    .roomImageUrl(imageUrls != null ? imageUrls.get("large") : null)
                    .hotel(hotel)
                    .build();

            Room saved = roomRepository.save(room);

            auditLogService.logHotel(actorId, "ADD_ROOM", hotel.getId(),
                    Map.of("roomNumber", saved.getRoomNumber(), "imageUrls", imageUrls));
            log.info("Room {} added to Hotel {} by Admin (ID: {})", saved.getRoomNumber(), hotel.getId(), actorId);

            return mapRoomToResponse(saved);

        } catch (Exception e) {
            log.error("Error adding room: {}", e.getMessage(), e);
            auditLogService.logHotel(actorId, "ADD_ROOM_ERROR", dto.getHotelId(),
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    @Override
    @Transactional
    public RoomResponseDTO updateRoom(Long roomId, RoomRequestDTO dto) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));

            if (dto.getRoomImageFile() != null && !dto.getRoomImageFile().isEmpty()) {
                safeDeleteImage(room.getRoomImageUrl());
                Map<String, String> imageUrls = cloudinaryService.uploadFileWithEagerSizes(dto.getRoomImageFile(), "rooms");
                room.setRoomImageUrl(imageUrls.get("large"));
            }

            room.setRoomNumber(dto.getRoomNumber());
            room.setType(dto.getType());
            room.setPricePerNight(dto.getPricePerNight());
            room.setAvailable(dto.isAvailable());

            Room updated = roomRepository.save(room);

            auditLogService.logHotel(actorId, "UPDATE_ROOM",
                    room.getHotel() != null ? room.getHotel().getId() : null,
                    Map.of("roomId", roomId));
            log.info("Room {} updated by Admin (ID: {})", roomId, actorId);

            return mapRoomToResponse(updated);

        } catch (Exception e) {
            log.error("Error updating room {}: {}", roomId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "UPDATE_ROOM_ERROR", roomId,
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {
        checkAdminAccess();
        Long actorId = getActorIdFromContext();

        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));

            safeDeleteImage(room.getRoomImageUrl());

            roomRepository.delete(room);

            auditLogService.logHotel(actorId, "DELETE_ROOM",
                    room.getHotel() != null ? room.getHotel().getId() : null,
                    Map.of("roomId", roomId));
            log.info("Room {} deleted (with image) by Admin (ID: {})", roomId, actorId);

        } catch (Exception e) {
            log.error("Error deleting room {}: {}", roomId, e.getMessage(), e);
            auditLogService.logHotel(actorId, "DELETE_ROOM_ERROR", null,
                    Map.of("error", e.getMessage()));
            throw e;
        }
    }

    @Override
    public List<RoomResponseDTO> getRoomsByHotel(Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
                .map(this::mapRoomToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceResponseDTO> getServicesForHotel(Long hotelId) {
        try {
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            List<com.justine.model.Service> services = serviceRepository.findByHotelId(hotelId);

            auditLogService.logHotel(
                    getActorIdFromContext(),
                    "GET_HOTEL_SERVICES_SUCCESS",
                    hotelId,
                    Map.of("serviceCount", services.size())
            );

            return services.stream()
                    .map(this::toServiceResponse)
                    .toList();

        } catch (Exception e) {
            log.error("Error fetching services for hotel {}: {}", hotelId, e.getMessage(), e);
            auditLogService.logHotel(getActorIdFromContext(),
                    "GET_HOTEL_SERVICES_ERROR",
                    hotelId,
                    Map.of("error", e.getMessage()));

            throw new RuntimeException("Failed to fetch hotel services", e);
        }
    }

    @Override
    @Transactional
    public ServiceResponseDTO addServiceToHotel(Long hotelId, CreateServiceRequest request) {
        try {
            checkAdminAccess();
            Long actorId = getActorIdFromContext();

            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));

            // Prevent duplicate services in same hotel
            boolean exists = serviceRepository.existsByHotelIdAndServiceType(
                    hotelId,
                    request.getServiceType()
            );

            if (exists) {
                auditLogService.logHotel(actorId,
                        "ADD_SERVICE_TO_HOTEL_DUPLICATE",
                        hotelId,
                        Map.of("serviceType", request.getServiceType()));
                throw new IllegalStateException("Service already exists in this hotel");
            }

            com.justine.model.Service service = com.justine.model.Service.builder()
                    .serviceType(request.getServiceType())
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice()) // may trigger @PrePersist defaults
                    .hotel(hotel)
                    .build();

            com.justine.model.Service saved = serviceRepository.save(service);

            auditLogService.logHotel(actorId,
                    "ADD_SERVICE_TO_HOTEL_SUCCESS",
                    hotelId,
                    Map.of("serviceId", saved.getId()));

            return toServiceResponse(saved);

        } catch (Exception e) {
            log.error("Error adding service {} to hotel {}: {}", request.getServiceType(), hotelId, e.getMessage(), e);

            auditLogService.logHotel(getActorIdFromContext(),
                    "ADD_SERVICE_TO_HOTEL_ERROR",
                    hotelId,
                    Map.of("serviceType", request.getServiceType(), "error", e.getMessage()));

            throw new RuntimeException("Failed to add service to hotel", e);
        }
    }

    @Override
    public RoomResponseDTO getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        return mapRoomToResponse(room);
    }

    @Override
    public List<RoomResponseDTO> getAvailableRooms(Long hotelId, String checkIn, String checkOut) {
        Long actorId = getActorIdFromContext();

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate checkInDate = LocalDate.parse(checkIn, formatter);
            LocalDate checkOutDate = LocalDate.parse(checkOut, formatter);

            List<Room> rooms = roomRepository.findByHotelId(hotelId);

            List<Room> availableRooms = rooms.stream()
                    .filter(room -> {
                        List<Booking> bookings = room.getBookings();
                        if (bookings == null || bookings.isEmpty()) return true;

                        return bookings.stream().noneMatch(booking -> {
                            if (booking.getStatus() != BookingStatus.BOOKED &&
                                    booking.getStatus() != BookingStatus.CHECKED_IN) {
                                return false;
                            }

                            LocalDate bookedCheckIn = booking.getCheckInDate();
                            LocalDate bookedCheckOut = booking.getCheckOutDate();

                            return !(checkOutDate.isBefore(bookedCheckIn) ||
                                    checkInDate.isAfter(bookedCheckOut));
                        });
                    })
                    .collect(Collectors.toList());

            return availableRooms.stream()
                    .map(room -> mapRoomToResponseForDateRange(room, checkInDate, checkOutDate))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching available rooms for Hotel {}: {}", hotelId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Special mapper ONLY for getAvailableRooms()
     * ✅ Sets availability based on actual date range instead of DB column
     */
    private RoomResponseDTO mapRoomToResponseForDateRange(Room room,
                                                          LocalDate checkIn,
                                                          LocalDate checkOut) {

        boolean isBookedThisRange =
                room.getBookings() != null &&
                        room.getBookings().stream().anyMatch(booking -> {
                            if (booking.getStatus() != BookingStatus.BOOKED &&
                                    booking.getStatus() != BookingStatus.CHECKED_IN) {
                                return false;
                            }

                            LocalDate bookedCheckIn = booking.getCheckInDate();
                            LocalDate bookedCheckOut = booking.getCheckOutDate();

                            return !(checkOut.isBefore(bookedCheckIn) ||
                                    checkIn.isAfter(bookedCheckOut));
                        });

        return RoomResponseDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .pricePerNight(room.getPricePerNight())
                .available(!isBookedThisRange) // ✅ Corrected availability!
                .roomImageUrl(room.getRoomImageUrl())
                .hotel(room.getHotel() != null ? HotelResponseDTO.builder()
                        .id(room.getHotel().getId())
                        .name(room.getHotel().getName())
                        .location(room.getHotel().getLocation())
                        .build() : null)
                .build();
    }

    /* ====================== READ METHODS ====================== */
    @Override
    public HotelResponseDTO getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found"));
        return mapToResponse(hotel);
    }

    @Override
    public List<HotelResponseDTO> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    /* ====================== MAPPERS ====================== */
    private HotelResponseDTO mapToResponse(Hotel hotel) {
        List<StaffResponseDTO> staffDTOs = hotel.getStaffMembers() != null
                ? hotel.getStaffMembers().stream()
                .map(staff -> StaffResponseDTO.builder()
                        .id(staff.getId())
                        .fullName(staff.getFullName())
                        .email(staff.getEmail())
                        .phoneNumber(staff.getPhoneNumber())
                        .role(staff.getRole())
                        .gender(staff.getGender())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return HotelResponseDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .location(hotel.getLocation())
                .contactNumber(hotel.getContactNumber())
                .email(hotel.getEmail())
                .hotelImageUrl(hotel.getHotelImageUrl())
                .rooms(hotel.getRooms() != null
                        ? hotel.getRooms().stream().map(this::mapRoomToResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .staffMembers(staffDTOs)
                .build();
    }



    private RoomResponseDTO mapRoomToResponse(Room room) {
        return RoomResponseDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .pricePerNight(room.getPricePerNight())
                .available(room.isAvailable())
                .roomImageUrl(room.getRoomImageUrl())
                .hotel(room.getHotel() != null ? HotelResponseDTO.builder()
                        .id(room.getHotel().getId())
                        .name(room.getHotel().getName())
                        .location(room.getHotel().getLocation())
                        .build() : null)
                .build();
    }

    private ServiceResponseDTO toServiceResponse(com.justine.model.Service service) {
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .serviceType(service.getServiceType())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .hotelId(service.getHotel() != null ? service.getHotel().getId() : null)
                .build();
    }

}
