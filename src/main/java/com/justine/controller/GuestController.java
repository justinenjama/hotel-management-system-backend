package com.justine.controller;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.service.GuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService){
        this.guestService = guestService;
    }

    private Long getCurrentUserId(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    @PostMapping
    public ResponseEntity<GuestResponseDTO> createGuest(@RequestBody GuestDTO dto) {
        return guestService.createGuest(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestResponseDTO> updateGuest(@PathVariable Long id,
                                                        @RequestBody GuestDTO dto,
                                                        Principal principal) {
        return guestService.updateGuest(id, dto, getCurrentUserId(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestResponseDTO> getGuest(@PathVariable Long id, Principal principal) {
        return guestService.getGuestById(id, getCurrentUserId(principal));
    }

    @GetMapping
    public ResponseEntity<List<GuestResponseDTO>> getAllGuests(Principal principal) {
        return guestService.getAllGuests(getCurrentUserId(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id, Principal principal) {
        return guestService.deleteGuest(id, getCurrentUserId(principal));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<GuestResponseDTO>> getGuestBookings(@PathVariable Long id, Principal principal) {
        return guestService.getGuestBookings(id, getCurrentUserId(principal));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<GuestResponseDTO>> getGuestOrders(@PathVariable Long id, Principal principal) {
        return guestService.getGuestOrders(id, getCurrentUserId(principal));
    }
}
