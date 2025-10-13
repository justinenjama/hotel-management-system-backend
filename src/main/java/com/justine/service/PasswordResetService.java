package com.justine.service;

import com.justine.dtos.request.PasswordResetRequestDto;
import com.justine.dtos.response.PasswordResetTokenDto;

public interface PasswordResetService {
    void requestPasswordReset(PasswordResetRequestDto request);
    void resetPassword(PasswordResetTokenDto request);
}
