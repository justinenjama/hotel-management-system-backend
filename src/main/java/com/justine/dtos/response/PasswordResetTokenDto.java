package com.justine.dtos.response;

import lombok.Data;

@Data
public class PasswordResetTokenDto {
    private String token;
    private String newPassword;
}
