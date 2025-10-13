package com.justine.service;

import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;

import java.util.List;

public interface HotelService {
    HotelResponseDTO createHotel(HotelRequestDTO dto);
    HotelResponseDTO updateHotel(Long id, HotelRequestDTO dto);
    void deleteHotel(Long id);
    HotelResponseDTO getHotelById(Long id);
    List<HotelResponseDTO> getAllHotels();

    RoomResponseDTO addRoomToHotel(RoomRequestDTO dto);
    RoomResponseDTO updateRoom(Long roomId, RoomRequestDTO dto);
    void deleteRoom(Long roomId);
}
