package com.justine.service;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface GuestService {
    ResponseEntity<GuestResponseDTO> createGuest(GuestDTO dto);

    ResponseEntity<GuestResponseDTO> updateGuest(Long guestId, GuestDTO dto, Long currentUserId);

    ResponseEntity<GuestResponseDTO> getGuestById(Long id, Long currentUserId);

    ResponseEntity<List<GuestResponseDTO>> getAllGuests(Long currentUserId);

    ResponseEntity<Void> deleteGuest(Long id, Long currentUserId);

    ResponseEntity<List<GuestResponseDTO>> getGuestBookings(Long guestId, Long currentUserId);

    ResponseEntity<List<GuestResponseDTO>> getGuestOrders(Long guestId, Long currentUserId);
}
