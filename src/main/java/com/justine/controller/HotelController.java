package com.justine.controller;

import com.justine.dtos.request.CreateServiceRequest;
import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.ApiResponse;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import com.justine.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Validated
public class HotelController {

    private final HotelService hotelService;

    /** ====================== HOTEL CRUD ====================== */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<HotelResponseDTO>> createHotel(
            @ModelAttribute @Validated HotelRequestDTO dto) {
        HotelResponseDTO response = hotelService.createHotel(dto);
        return ResponseEntity.ok(ApiResponse.success("Hotel created successfully", response));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<HotelResponseDTO>> updateHotel(
            @PathVariable Long id,
            @ModelAttribute @Validated HotelRequestDTO dto) {
        HotelResponseDTO response = hotelService.updateHotel(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Hotel updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponseDTO>> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hotel retrieved successfully", hotelService.getHotelById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponseDTO>>> getAllHotels() {
        return ResponseEntity.ok(ApiResponse.success("All hotels retrieved", hotelService.getAllHotels()));
    }

    /** ====================== ROOM CRUD ====================== */

    @PostMapping(value = "/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomResponseDTO>> addRoom(
            @ModelAttribute @Validated RoomRequestDTO dto) {
        RoomResponseDTO response = hotelService.addRoomToHotel(dto);
        return ResponseEntity.ok(ApiResponse.success("Room added successfully", response));
    }

    @PutMapping(value = "/rooms/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomResponseDTO>> updateRoom(
            @PathVariable Long roomId,
            @ModelAttribute @Validated RoomRequestDTO dto) {
        RoomResponseDTO response = hotelService.updateRoom(roomId, dto);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", response));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long roomId) {
        hotelService.deleteRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    @GetMapping("/{hotelId}/rooms")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByHotel(@PathVariable Long hotelId) {
        List<RoomResponseDTO> rooms = hotelService.getRoomsByHotel(hotelId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponseDTO> getRoomById(@PathVariable Long roomId) {
        RoomResponseDTO room = hotelService.getRoomById(roomId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{hotelId}/available-rooms")
    public ResponseEntity<List<RoomResponseDTO>> getAvailableRooms(
            @PathVariable Long hotelId,
            @RequestParam String checkIn,
            @RequestParam String checkOut
    ) {
        List<RoomResponseDTO> availableRooms = hotelService.getAvailableRooms(hotelId, checkIn, checkOut);
        return ResponseEntity.ok(availableRooms);
    }

    /** ====================== SERVICE ENDPOINTS ====================== */

    // Get all services for a hotel
    @GetMapping("/{hotelId}/services")
    public ResponseEntity<List<ServiceResponseDTO>> getServicesForHotel(@PathVariable Long hotelId) {
        List<ServiceResponseDTO> services = hotelService.getServicesForHotel(hotelId);
        return ResponseEntity.ok(services);
    }

    // Add a service to a hotel
    @PostMapping("/{hotelId}/services")
    public ResponseEntity<ServiceResponseDTO> addServiceToHotel(
            @PathVariable Long hotelId,
            @RequestBody CreateServiceRequest request
            ) {
        ServiceResponseDTO service = hotelService.addServiceToHotel(hotelId, request);
        return ResponseEntity.ok(service);
    }
}
