package com.justine.controller;

import com.justine.dtos.request.HotelRequestDTO;
import com.justine.dtos.request.RoomRequestDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.RoomResponseDTO;
import com.justine.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelResponseDTO> createHotel(@RequestBody HotelRequestDTO dto) {
        return ResponseEntity.ok(hotelService.createHotel(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelResponseDTO> updateHotel(@PathVariable Long id, @RequestBody HotelRequestDTO dto) {
        return ResponseEntity.ok(hotelService.updateHotel(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponseDTO> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    @GetMapping
    public ResponseEntity<List<HotelResponseDTO>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponseDTO> addRoom(@RequestBody RoomRequestDTO dto) {
        return ResponseEntity.ok(hotelService.addRoomToHotel(dto));
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponseDTO> updateRoom(@PathVariable Long roomId, @RequestBody RoomRequestDTO dto) {
        return ResponseEntity.ok(hotelService.updateRoom(roomId, dto));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        hotelService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
