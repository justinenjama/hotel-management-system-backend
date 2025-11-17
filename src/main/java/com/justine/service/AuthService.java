package com.justine.service;

import com.justine.dtos.request.ChangePasswordRequestDTO;
import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.request.LoginRequestDTO;
import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.enums.StaffRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface AuthService {

    ResponseEntity<?> registerGuest(GuestDTO guestDTO);


    // ---------------- LOGIN ---------------- //
    ResponseEntity<?> login(LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response);

    ResponseEntity<?> getCurrentUser(HttpServletRequest request);

    // ---------------- REGISTER STAFF ---------------- //
    ResponseEntity<?> registerStaff(StaffRequestDTO dto, Authentication authentication);

    ResponseEntity<?> updateUser(Long id, Map<String, Object> updates, Authentication authentication);

    ResponseEntity<?> changePassword(Long userId, ChangePasswordRequestDTO request, Authentication authentication);

    // ---------------- REFRESH TOKEN ---------------- //
    ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response, String refreshToken);

    // ---------------- LOGOUT ---------------- //
    ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response);

    ResponseEntity<List<GuestResponseDTO>> getAllUsers();
}
