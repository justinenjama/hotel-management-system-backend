package com.justine.serviceImpl;

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
import com.justine.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public AuthServiceImpl(
            GuestRepository guestRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuditLogService auditLogService,
            EmailService emailService
    ) {
        this.guestRepository = guestRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    // ---------------- REGISTER GUEST ---------------- //
    @Override
    public ResponseEntity<?> registerGuest(GuestDTO guestDTO) {
        try {
            if (guestRepository.existsByEmail(guestDTO.getEmail()))
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");

            if (guestRepository.existsByPhoneNumber(guestDTO.getPhoneNumber()))
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Phone number already in use");

            if (!guestDTO.getPassword().equals(guestDTO.getConfirmPassword()))
                return ResponseEntity.badRequest().body("Passwords do not match");

            Guest guest = Guest.builder()
                    .fullName(guestDTO.getFullName())
                    .email(guestDTO.getEmail())
                    .phoneNumber(guestDTO.getPhoneNumber())
                    .idNumber(guestDTO.getIdNumber())
                    .password(passwordEncoder.encode(guestDTO.getPassword()))
                    .gender(guestDTO.getGender())
                    .role("GUEST")
                    .build();

            guestRepository.saveAndFlush(guest);

            auditLogService.logAuthService(null, "REGISTER_GUEST_SUCCESS", Map.of(
                    "email", guest.getEmail(), "name", guest.getFullName()
            ));

            emailService.sendEmail(
                    guest.getEmail(),
                    "Welcome to Justine Hotels",
                    String.format("<p>Hi %s,</p><p>Welcome to Justine Hotels!</p>", guest.getFullName())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body("Guest registered successfully");
        } catch (Exception e) {
            log.error("Register guest error: {}", e.getMessage());
            auditLogService.logAuthService(null, "REGISTER_GUEST_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.internalServerError().body("Registration failed");
        }
    }

    // ---------------- REGISTER STAFF ---------------- //
    @Override
    public ResponseEntity<?> registerStaff(GuestDTO dto, StaffRole role) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin)
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can register staff");

            if (staffRepository.existsByEmail(dto.getEmail()))
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already used");

            if (!dto.getPassword().equals(dto.getConfirmPassword()))
                return ResponseEntity.badRequest().body("Passwords do not match");

            Staff staff = Staff.builder()
                    .fullName(dto.getFullName())
                    .email(dto.getEmail())
                    .phoneNumber(dto.getPhoneNumber())
                    .gender(dto.getGender())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .role(role)
                    .build();

            staffRepository.saveAndFlush(staff);

            auditLogService.logAuthService(null, "REGISTER_STAFF_SUCCESS", Map.of(
                    "email", staff.getEmail(), "role", staff.getRole().name()
            ));

            emailService.sendEmail(
                    staff.getEmail(),
                    "Your Staff Account",
                    String.format("<p>Hi %s,</p><p>Your account has been created with role <b>%s</b>.</p>", staff.getFullName(), staff.getRole())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body("Staff registered successfully");
        } catch (Exception e) {
            log.error("Register staff error: {}", e.getMessage());
            auditLogService.logAuthService(null, "REGISTER_STAFF_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.internalServerError().body("Staff registration failed");
        }
    }

    // ---------------- UPDATE USER ---------------- //
    @Override
    public ResponseEntity<?> updateUser(Long id, Map<String, Object> updates, Authentication authentication) {
        try {
            String authUserId = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().contains("STAFF") || a.getAuthority().contains("MANAGER"));

            if (!isAdmin && !authUserId.equals(String.valueOf(id))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only update your own profile"));
            }

            if (isStaff) {
                Optional<Staff> staffOpt = staffRepository.findById(id);
                if (staffOpt.isEmpty())
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Staff not found"));

                Staff staff = staffOpt.get();

                if (updates.containsKey("fullName")) staff.setFullName((String) updates.get("fullName"));
                if (updates.containsKey("phoneNumber")) staff.setPhoneNumber((String) updates.get("phoneNumber"));
                if (updates.containsKey("gender")) staff.setGender((String) updates.get("gender"));

                if (updates.containsKey("role")) {
                    if (!isAdmin)
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can update roles"));
                    try {
                        staff.setRole(StaffRole.valueOf(((String) updates.get("role")).toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
                    }
                }

                staffRepository.save(staff);
                auditLogService.logAuthService(staff.getId(), "UPDATE_PROFILE_SUCCESS", Map.of("id", id, "type", "STAFF"));
                return buildStaffResponse(staff);

            } else {
                Optional<Guest> guestOpt = guestRepository.findById(id);
                if (guestOpt.isEmpty())
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Guest not found"));

                Guest guest = guestOpt.get();

                if (updates.containsKey("fullName")) guest.setFullName((String) updates.get("fullName"));
                if (updates.containsKey("phoneNumber")) guest.setPhoneNumber((String) updates.get("phoneNumber"));
                if (updates.containsKey("gender")) guest.setGender((String) updates.get("gender"));

                guestRepository.save(guest);
                auditLogService.logAuthService(guest.getId(), "UPDATE_PROFILE_SUCCESS", Map.of("id", id, "type", "GUEST"));
                return buildGuestResponse(guest);
            }

        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            auditLogService.logAuthService(null, "UPDATE_PROFILE_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update profile"));
        }
    }

    // ---------------- LOGIN ---------------- //
    @Override
    public ResponseEntity<?> login(LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            Optional<Staff> staffOpt = staffRepository.findByEmail(loginRequest.getEmail());
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                if (!passwordEncoder.matches(loginRequest.getPassword(), staff.getPassword()))
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");

                String access = jwtUtils.generateToken(staff);
                String refresh = jwtUtils.generateRefreshToken(staff);

                response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateAccessTokenCookie(access).toString());
                response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(refresh).toString());

                auditLogService.logAuthService(staff.getId(), "LOGIN_SUCCESS",
                        Map.of("role", staff.getRole().name(), "email", staff.getEmail()));
                return ResponseEntity.ok("Staff logged in successfully");
            }

            Guest guest = guestRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            if (!passwordEncoder.matches(loginRequest.getPassword(), guest.getPassword()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");

            String access = jwtUtils.generateToken(guest);
            String refresh = jwtUtils.generateRefreshToken(guest);

            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateAccessTokenCookie(access).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(refresh).toString());

            auditLogService.logAuthService(guest.getId(), "LOGIN_SUCCESS", Map.of("email", guest.getEmail()));
            return ResponseEntity.ok("Guest logged in successfully");

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            auditLogService.logAuthService(null, "LOGIN_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.internalServerError().body("Login failed");
        }
    }

    // ---------------- REFRESH TOKEN ---------------- //
    @Override
    public ResponseEntity<?> refreshToken(HttpServletResponse response, String refreshToken) {
        try {
            if (!jwtUtils.isTokenValid(refreshToken))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

            String userId = jwtUtils.extractSubject(refreshToken);
            boolean isStaff = jwtUtils.extractRoles(refreshToken).stream()
                    .anyMatch(r -> r.contains("STAFF") || r.contains("MANAGER"));

            String newAccessToken = isStaff
                    ? jwtUtils.generateToken(staffRepository.findById(Long.parseLong(userId)).orElseThrow())
                    : jwtUtils.generateToken(guestRepository.findById(Long.parseLong(userId)).orElseThrow());

            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateAccessTokenCookie(newAccessToken).toString());

            auditLogService.logAuthService(Long.parseLong(userId), "REFRESH_TOKEN_SUCCESS", Map.of("id", userId));
            return ResponseEntity.ok("Access token refreshed");

        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage());
            auditLogService.logAuthService(null, "REFRESH_TOKEN_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.internalServerError().body("Refresh failed");
        }
    }

    // ------------------ Get Current User ------------------
    @Override
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 1️⃣ Extract JWT from request cookies or Authorization header
            String token = jwtUtils.extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No token found"));
            }

            // 2️⃣ Extract user ID and roles from JWT
            Long userId = Long.parseLong(jwtUtils.getSubject(token));
            List<String> roles = jwtUtils.getRoles(token);

            // 3️⃣ Determine if user is staff (roles containing ADMIN, MANAGER, STAFF, RECEPTIONIST)
            boolean isStaff = roles.stream().anyMatch(role ->
                    role.equalsIgnoreCase("ADMIN") ||
                            role.equalsIgnoreCase("MANAGER") ||
                            role.equalsIgnoreCase("STAFF") ||
                            role.equalsIgnoreCase("RECEPTIONIST")
            );

            // 4️⃣ Fetch user from DB
            if (isStaff) {
                Staff staff = staffRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Staff not found"));
                return buildStaffResponse(staff);
            } else {
                Guest guest = guestRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Guest not found"));
                return buildGuestResponse(guest);
            }

        } catch (NumberFormatException e) {
            log.error("Invalid user ID in token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid token subject"));
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            auditLogService.logAuthService(null, "GET_CURRENT_USER_ERROR", Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch user"));
        }
    }

    // ---------------- LOGOUT ---------------- //
    @Override
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.clearRefreshTokenCookie().toString());
        auditLogService.logAuthService(null, "LOGOUT_SUCCESS", Map.of("message", "User logged out"));
        return ResponseEntity.ok("Logged out successfully");
    }

    // ---------------- HELPERS ---------------- //
    private ResponseEntity<?> buildStaffResponse(Staff staff) {
        StaffResponseDTO dto = StaffResponseDTO.builder()
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
                        .build() : null)
                .build();
        return ResponseEntity.ok(Map.of("userType", "STAFF", "user", dto));
    }

    private ResponseEntity<?> buildGuestResponse(Guest guest) {
        GuestResponseDTO dto = GuestResponseDTO.builder()
                .id(guest.getId())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .phoneNumber(guest.getPhoneNumber())
                .idNumber(guest.getIdNumber())
                .gender(guest.getGender())
                .role(guest.getRole())
                .build();
        return ResponseEntity.ok(Map.of("userType", "GUEST", "user", dto));
    }
}
