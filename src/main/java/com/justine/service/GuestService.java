package com.justine.service;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface GuestService {
    ResponseEntity<GuestResponseDTO> createGuest(GuestDTO dto);

    ResponseEntity<GuestResponseDTO> updateGuest(Long guestId, GuestDTO dto, String currentUserEmail);

    ResponseEntity<GuestResponseDTO> getGuestById(Long id, String currentUserEmail);

    ResponseEntity<List<GuestResponseDTO>> getAllGuests(String currentUserEmail);

    ResponseEntity<Void> deleteGuest(Long id, String currentUserEmail);

    ResponseEntity<List<GuestResponseDTO>> getGuestBookings(Long guestId, String currentUserEmail);

    ResponseEntity<List<GuestResponseDTO>> getGuestOrders(Long guestId, String currentUserEmail);
}
