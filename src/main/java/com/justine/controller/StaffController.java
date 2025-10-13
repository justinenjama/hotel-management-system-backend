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
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<StaffResponseDTO> addStaff(@RequestBody StaffRequestDTO dto, Principal principal) {
        return staffService.addStaff(dto, principal.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateStaff(@PathVariable Long id,
                                                        @RequestBody StaffRequestDTO dto,
                                                        Principal principal) {
        return staffService.updateStaff(id, dto, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id, Principal principal) {
        return staffService.deleteStaff(id, principal.getName());
    }

    @PutMapping("/{id}/assign-hotel/{hotelId}")
    public ResponseEntity<StaffResponseDTO> assignStaffToHotel(@PathVariable Long id,
                                                               @PathVariable Long hotelId,
                                                               Principal principal) {
        return staffService.assignStaffToHotel(id, hotelId, principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> getStaffById(@PathVariable Long id, Principal principal) {
        return staffService.getStaffById(id, principal.getName());
    }

    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff(Principal principal) {
        return staffService.getAllStaff(principal.getName());
    }
}
