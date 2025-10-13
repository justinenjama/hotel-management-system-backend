package com.justine.controller;

import com.justine.dtos.request.PasswordResetRequestDto;
import com.justine.dtos.response.PasswordResetTokenDto;
import com.justine.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<String> requestReset(@RequestBody PasswordResetRequestDto request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok("Password reset email sent.");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetTokenDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }
}
