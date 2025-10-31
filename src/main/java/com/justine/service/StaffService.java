package com.justine.service;

import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.StaffResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StaffService {
    ResponseEntity<StaffResponseDTO> addStaff(StaffRequestDTO dto, Long currentUserId);
    ResponseEntity<StaffResponseDTO> updateStaff(Long staffId, StaffRequestDTO dto, Long currentUserId);
    ResponseEntity<Void> deleteStaff(Long staffId, Long currentUserId);
    ResponseEntity<StaffResponseDTO> assignStaffToHotel(Long staffId, Long hotelId, Long currentUserId);
    ResponseEntity<StaffResponseDTO> getStaffById(Long staffId, Long currentUserId);
    ResponseEntity<List<StaffResponseDTO>> getAllStaff(Long currentUserId);
}
