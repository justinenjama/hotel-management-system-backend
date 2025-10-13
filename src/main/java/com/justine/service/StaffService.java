package com.justine.service;

import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.StaffResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StaffService {
    ResponseEntity<StaffResponseDTO> addStaff(StaffRequestDTO dto, String currentUserEmail);
    ResponseEntity<StaffResponseDTO> updateStaff(Long staffId, StaffRequestDTO dto, String currentUserEmail);
    ResponseEntity<Void> deleteStaff(Long staffId, String currentUserEmail);
    ResponseEntity<StaffResponseDTO> assignStaffToHotel(Long staffId, Long hotelId, String currentUserEmail);
    ResponseEntity<StaffResponseDTO> getStaffById(Long staffId, String currentUserEmail);
    ResponseEntity<List<StaffResponseDTO>> getAllStaff(String currentUserEmail);
}
