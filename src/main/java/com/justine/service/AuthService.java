package com.justine.service;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.request.LoginRequestDTO;
import com.justine.enums.StaffRole;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface AuthService {

    ResponseEntity<?> registerGuest(GuestDTO guestDTO);

    ResponseEntity<?> login(LoginRequestDTO loginRequest, HttpServletResponse response);

    ResponseEntity<?> logout(HttpServletResponse response);

    ResponseEntity<?> refreshToken(HttpServletResponse response, String refreshToken);

    ResponseEntity<?> getCurrentUserById(String userId, boolean isStaff);

    ResponseEntity<?> registerStaff(GuestDTO staffDTO, StaffRole role);

    ResponseEntity<?> updateUser(Long id, Map<String, Object> updates, Authentication authentication);
}
