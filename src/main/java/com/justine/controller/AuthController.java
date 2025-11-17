package com.justine.controller;

import com.justine.dtos.request.ChangePasswordRequestDTO;
import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.request.LoginRequestDTO;
import com.justine.dtos.request.StaffRequestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.enums.StaffRole;
import com.justine.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerGuest(@RequestBody GuestDTO guestDTO) {
        return authService.registerGuest(guestDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register-staff")
    public ResponseEntity<?> registerStaff(
            @RequestBody StaffRequestDTO staffDTO,
            Authentication authentication
    ) {
        return authService.registerStaff(staffDTO, authentication);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response) {
        return authService.login(loginRequest, request, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return authService.logout(request, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request, HttpServletResponse response, @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    ) {
        return authService.refreshToken(request, response, refreshToken);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        return authService.getCurrentUser(request);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthenticated"));
        }

        return authService.updateUser(id, updates, authentication);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthenticated"));
        }
        Long userId = Long.parseLong(authentication.getName());
        return authService.changePassword(userId, request, authentication);
    }

    @GetMapping("/users")
    public ResponseEntity<List<GuestResponseDTO>> getAllGuests(){
        return authService.getAllUsers();
    }


}
