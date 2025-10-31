package com.justine.controller;

import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.StaffResponseDTO;
import com.justine.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    public ResponseEntity<StaffResponseDTO> addStaff(@RequestBody StaffRequestDTO dto, Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.addStaff(dto, currentUserId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateStaff(@PathVariable Long id,
                                                        @RequestBody StaffRequestDTO dto,
                                                        Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.updateStaff(id, dto, currentUserId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id, Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.deleteStaff(id, currentUserId);
    }

    @PutMapping("/{id}/assign-hotel/{hotelId}")
    public ResponseEntity<StaffResponseDTO> assignStaffToHotel(@PathVariable Long id,
                                                               @PathVariable Long hotelId,
                                                               Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.assignStaffToHotel(id, hotelId, currentUserId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> getStaffById(@PathVariable Long id, Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.getStaffById(id, currentUserId);
    }

    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff(Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        return staffService.getAllStaff(currentUserId);
    }
}
