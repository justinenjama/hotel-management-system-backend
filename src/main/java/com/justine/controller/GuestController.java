package com.justine.controller;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PostMapping
    public ResponseEntity<GuestResponseDTO> createGuest(@RequestBody GuestDTO dto) {
        return guestService.createGuest(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestResponseDTO> updateGuest(@PathVariable Long id,
                                                        @RequestBody GuestDTO dto,
                                                        Principal principal) {
        return guestService.updateGuest(id, dto, principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestResponseDTO> getGuest(@PathVariable Long id, Principal principal) {
        return guestService.getGuestById(id, principal.getName());
    }

    @GetMapping
    public ResponseEntity<List<GuestResponseDTO>> getAllGuests(Principal principal) {
        return guestService.getAllGuests(principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id, Principal principal) {
        return guestService.deleteGuest(id, principal.getName());
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<GuestResponseDTO>> getGuestBookings(@PathVariable Long id, Principal principal) {
        return guestService.getGuestBookings(id, principal.getName());
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<GuestResponseDTO>> getGuestOrders(@PathVariable Long id, Principal principal) {
        return guestService.getGuestOrders(id, principal.getName());
    }
}
