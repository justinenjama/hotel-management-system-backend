package com.justine.serviceImpl;

import com.justine.dtos.request.PasswordResetRequestDto;
import com.justine.dtos.response.PasswordResetTokenDto;
import com.justine.model.PasswordResetToken;
import com.justine.model.Guest;
import com.justine.model.Staff;
import com.justine.repository.PasswordResetTokenRepository;
import com.justine.repository.GuestRepository;
import com.justine.repository.StaffRepository;
import com.justine.service.AuditLogService;
import com.justine.service.EmailService;
import com.justine.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;

    private static final int EXPIRATION_MINUTES = 15;
    private static final int RATE_LIMIT_SECONDS = 60; // 1 request per minute
    private final Map<String, LocalDateTime> lastRequestTime = new HashMap<>();

    private enum AuditAction {
        REQUEST, RESET, FAILED
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();

        // Rate limit check
        LocalDateTime lastRequested = lastRequestTime.get(email);
        if (lastRequested != null && lastRequested.plusSeconds(RATE_LIMIT_SECONDS).isAfter(LocalDateTime.now())) {
            auditLogService.logPasswordResetAction(
                    httpServletRequest,
                    AuditAction.FAILED.name(),
                    "Password reset requested too soon for email: " + email
            );
            return;
        }

        Optional<Guest> guestOpt = guestRepository.findByEmail(email);
        Optional<Staff> staffOpt = staffRepository.findByEmail(email);

        if (guestOpt.isEmpty() && staffOpt.isEmpty()) {
            log.warn("Password reset requested for unknown email: {}", email);
            auditLogService.logPasswordResetAction(
                    httpServletRequest,
                    AuditAction.FAILED.name(),
                    "Password reset requested for non-existent email: " + email
            );
            return; // Prevent email enumeration
        }

        String fullName;
        if (guestOpt.isPresent()) {
            fullName = guestOpt.get().getFullName();
        } else {
            fullName = staffOpt.get().getFullName();
        }

        // Invalidate old tokens
        tokenRepository.markAllTokensUsedForEmail(email);

        // Generate and save new token
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = BCrypt.hashpw(rawToken, BCrypt.gensalt());

        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(hashedToken)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .used(false)
                .build();
        tokenRepository.save(token);

        String resetLink = "https://justine-hotel.vercel.app/reset-password?token=" + rawToken;

        String message = """
                <p>Hello %s,</p>
                <p>You requested to reset your password. Click the link below to set a new password:</p>
                <p><a href="%s">Reset Password</a></p>
                <p>This link is valid for %d minutes.</p>
                <p>If you did not request this, please ignore this email.</p>
                """.formatted(fullName, resetLink, EXPIRATION_MINUTES);

        emailService.sendEmail(email, "Password Reset Request", message);

        auditLogService.logPasswordResetAction(
                httpServletRequest,
                AuditAction.REQUEST.name(),
                "Password reset link generated and sent to: " + email
        );

        lastRequestTime.put(email, LocalDateTime.now());
        log.info("Password reset link sent to {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetTokenDto request) {
        String rawToken = request.getToken();

        try {
            PasswordResetToken resetToken = tokenRepository.findAll().stream()
                    .filter(token -> !token.isUsed()
                            && token.getExpiresAt().isAfter(LocalDateTime.now())
                            && BCrypt.checkpw(rawToken, token.getTokenHash()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Invalid or expired token."));

            String email = resetToken.getEmail();

            Optional<Guest> guestOpt = guestRepository.findByEmail(email);
            Optional<Staff> staffOpt = staffRepository.findByEmail(email);

            if (guestOpt.isEmpty() && staffOpt.isEmpty()) {
                throw new RuntimeException("User not found for email: " + email);
            }

            if (guestOpt.isPresent()) {
                Guest guest = guestOpt.get();
                guest.setPassword(passwordEncoder.encode(request.getNewPassword()));
                guestRepository.save(guest);
            } else {
                Staff staff = staffOpt.get();
                staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
                staffRepository.save(staff);
            }

            resetToken.setUsed(true);
            tokenRepository.save(resetToken);

            String confirmationMsg = """
                    <p>Hello,</p>
                    <p>Your password has been successfully reset.</p>
                    <p>If this wasn’t you, please contact support immediately.</p>
                    """;

            emailService.sendEmail(email, "Password Reset Successful", confirmationMsg);

            auditLogService.logPasswordResetAction(
                    httpServletRequest,
                    AuditAction.RESET.name(),
                    "Password successfully reset for email: " + email
            );

            log.info("Password reset successful for {}", email);

        } catch (RuntimeException e) {
            auditLogService.logPasswordResetAction(
                    httpServletRequest,
                    AuditAction.FAILED.name(),
                    "Failed password reset attempt: " + e.getMessage()
            );
            log.warn("Failed password reset attempt: {}", e.getMessage());
            throw e;
        }
    }
}
