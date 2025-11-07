package com.justine.service;

import com.justine.dtos.request.CreateServiceRequest;
import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface HotelService {
    // ====== Hotel Management ======
    HotelResponseDTO createHotel(HotelRequestDTO dto);
    HotelResponseDTO updateHotel(Long id, HotelRequestDTO dto);
    void deleteHotel(Long id);
    HotelResponseDTO getHotelById(Long id);
    List<HotelResponseDTO> getAllHotels();

    // ====== Room Management ======
    RoomResponseDTO addRoomToHotel(RoomRequestDTO dto);
    RoomResponseDTO updateRoom(Long roomId, RoomRequestDTO dto);
    void deleteRoom(Long roomId);

    List<RoomResponseDTO> getRoomsByHotel(Long hotelId);

    List<ServiceResponseDTO> getServicesForHotel(Long hotelId);
    ServiceResponseDTO addServiceToHotel(Long hotelId, CreateServiceRequest request);

    RoomResponseDTO getRoomById(Long roomId);

    List<RoomResponseDTO> getAvailableRooms(Long hotelId, String checkIn, String checkOut);
}
