package com.justine.serviceImpl;

import java.util.Map;
import java.util.Optional;

import com.justine.dtos.request.GuestDTO;
import com.justine.dtos.request.LoginRequestDTO;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.dtos.response.HotelResponseDTO;
import com.justine.dtos.response.StaffResponseDTO;
import com.justine.enums.StaffRole;
import com.justine.model.Guest;
import com.justine.model.Staff;
import com.justine.repository.GuestRepository;
import com.justine.repository.StaffRepository;
import com.justine.security.JwtUtils;
import com.justine.service.AuditLogService;
import com.justine.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    // ------------------ Register Guest ------------------
    @Override
    public ResponseEntity<?> registerGuest(GuestDTO guestDTO) {
        try {
            if (guestRepository.existsByEmail(guestDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use");
            }

            if (guestRepository.existsByPhoneNumber(guestDTO.getPhoneNumber())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Phone Number is already in use");
            }

            if (!guestDTO.getPassword().equals(guestDTO.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
            }

            Guest newGuest = Guest.builder()
                    .fullName(guestDTO.getFullName())
                    .email(guestDTO.getEmail())
                    .phoneNumber(guestDTO.getPhoneNumber())
                    .idNumber(guestDTO.getIdNumber())
                    .password(passwordEncoder.encode(guestDTO.getPassword()))
                    .gender(guestDTO.getGender())
                    .role("GUEST")
                    .build();

            guestRepository.save(newGuest);

            auditLogService.logAuthService(null, "REGISTER_GUEST_SUCCESS", Map.of(
                    "email", newGuest.getEmail(),
                    "name", newGuest.getFullName()
            ));

            log.info("Guest registered successfully: {}", newGuest.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body("Guest registered successfully");

        } catch (Exception e) {
            log.error("Error registering guest: {}", e.getMessage());
            auditLogService.logAuthService(null, "REGISTER_GUEST_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Guest registration failed");
        }
    }

    // ------------------ Register Staff (Admin Only) ------------------
    @Override
    public ResponseEntity<?> registerStaff(GuestDTO staffDTO, StaffRole role) {
        try {
            // Verify admin
            var auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can register staff");
            }

            if (staffRepository.existsByEmail(staffDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use");
            }

            if (!staffDTO.getPassword().equals(staffDTO.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
            }

            Staff newStaff = Staff.builder()
                    .fullName(staffDTO.getFullName())
                    .email(staffDTO.getEmail())
                    .phoneNumber(staffDTO.getPhoneNumber())
                    .gender(staffDTO.getGender())
                    .password(passwordEncoder.encode(staffDTO.getPassword()))
                    .role(role)
                    .build();

            staffRepository.save(newStaff);

            auditLogService.logAuthService(null, "REGISTER_STAFF_SUCCESS", Map.of(
                    "email", newStaff.getEmail(),
                    "role", newStaff.getRole().name()
            ));

            log.info("Staff registered successfully: {}", newStaff.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body("Staff registered successfully");

        } catch (Exception e) {
            log.error("Error registering staff: {}", e.getMessage());
            auditLogService.logAuthService(null, "REGISTER_STAFF_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Staff registration failed");
        }
    }

    // ------------------ Login ------------------
    @Override
    public ResponseEntity<?> login(LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            // Staff Login
            Optional<Staff> staffOpt = staffRepository.findByEmail(loginRequest.getEmail());
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                if (!passwordEncoder.matches(loginRequest.getPassword(), staff.getPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
                }

                String accessToken = jwtUtils.generateToken(staff);
                String refreshToken = jwtUtils.generateRefreshToken(staff);

                response.addCookie(jwtUtils.generateAccessTokenCookie(accessToken));
                response.addCookie(jwtUtils.generateRefreshTokenCookie(refreshToken));

                auditLogService.logAuthService(staff.getId(), "LOGIN_SUCCESS", Map.of(
                        "email", staff.getEmail(),
                        "role", staff.getRole().name()
                ));

                return ResponseEntity.ok("Staff logged in successfully");
            }

            // Guest Login
            Optional<Guest> guestOpt = guestRepository.findByEmail(loginRequest.getEmail());
            if (guestOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            Guest guest = guestOpt.get();
            if (!passwordEncoder.matches(loginRequest.getPassword(), guest.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            String accessToken = jwtUtils.generateToken(guest);
            String refreshToken = jwtUtils.generateRefreshToken(guest);

            response.addCookie(jwtUtils.generateAccessTokenCookie(accessToken));
            response.addCookie(jwtUtils.generateRefreshTokenCookie(refreshToken));

            auditLogService.logAuthService(guest.getId(), "LOGIN_SUCCESS", Map.of(
                    "email", guest.getEmail(),
                    "role", guest.getRole()
            ));

            return ResponseEntity.ok("Guest logged in successfully");

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            auditLogService.logAuthService(null, "LOGIN_ERROR", Map.of(
                    "email", loginRequest.getEmail(),
                    "error", e.getMessage()
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed");
        }
    }

    // ------------------ Logout ------------------
    @Override
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            response.addCookie(jwtUtils.clearAccessTokenCookie());
            response.addCookie(jwtUtils.clearRefreshTokenCookie());

            auditLogService.logAuthService(null, "LOGOUT_SUCCESS", Map.of("message", "User logged out"));
            return ResponseEntity.ok("Logged out successfully");

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            auditLogService.logAuthService(null, "LOGOUT_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }

    // ------------------ Refresh Token ------------------
    @Override
    public ResponseEntity<?> refreshToken(HttpServletResponse response, String refreshToken) {
        try {
            if (!jwtUtils.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            String userId = jwtUtils.extractSubject(refreshToken);
            boolean isStaff = jwtUtils.extractRoles(refreshToken).stream()
                    .anyMatch(r -> r.startsWith("MANAGER") || r.startsWith("STAFF"));

            String newAccessToken;
            if (isStaff) {
                Staff staff = staffRepository.findById(Long.parseLong(userId)).orElseThrow();
                newAccessToken = jwtUtils.generateToken(staff);
            } else {
                Guest guest = guestRepository.findById(Long.parseLong(userId)).orElseThrow();
                newAccessToken = jwtUtils.generateToken(guest);
            }

            response.addCookie(jwtUtils.generateAccessTokenCookie(newAccessToken));
            auditLogService.logAuthService(Long.parseLong(userId), "REFRESH_TOKEN_SUCCESS", Map.of("userId", userId));

            return ResponseEntity.ok("Access token refreshed");

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            auditLogService.logAuthService(null, "REFRESH_TOKEN_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token refresh failed");
        }
    }

    // ------------------ Get Current User ------------------
    @Override
    public ResponseEntity<?> getCurrentUserById(String userId, boolean isStaff) {
        try {
            Long id = Long.parseLong(userId);

            if (isStaff) {
                Optional<Staff> staffOpt = staffRepository.findById(id);
                if (staffOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Staff not found"));
                }
                Staff staff = staffOpt.get();
                return buildStaffResponse(staff);
            } else {
                Optional<Guest> guestOpt = guestRepository.findById(id);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Guest not found"));
                }
                Guest guest = guestOpt.get();
                return buildGuestResponse(guest);
            }
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            auditLogService.logAuthService(null, "GET_CURRENT_USER_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch user");
        }
    }

    // ------------------ Helpers ------------------
    private ResponseEntity<?> buildStaffResponse(Staff staff) {
        var staffResponse = StaffResponseDTO.builder()
                .id(staff.getId())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .gender(staff.getGender())
                .role(staff.getRole())
                .hotel(staff.getHotel() != null
                        ? HotelResponseDTO.builder()
                        .id(staff.getHotel().getId())
                        .name(staff.getHotel().getName())
                        .location(staff.getHotel().getLocation())
                        .contactNumber(staff.getHotel().getContactNumber())
                        .email(staff.getHotel().getEmail())
                        .build()
                        : null)
                .build();

        return ResponseEntity.ok(Map.of("type", "STAFF", "data", staffResponse));
    }

    private ResponseEntity<?> buildGuestResponse(Guest guest) {
        var guestResponse = GuestResponseDTO.builder()
                .id(guest.getId())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .phoneNumber(guest.getPhoneNumber())
                .idNumber(guest.getIdNumber())
                .gender(guest.getGender())
                .role(guest.getRole())
                .build();

        return ResponseEntity.ok(Map.of("type", "GUEST", "data", guestResponse));
    }
}
