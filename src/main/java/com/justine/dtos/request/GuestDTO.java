package com.justine.dtos.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestDTO {
    private String fullName;
    private String email;
    private String idNumber;
    private String phoneNumber;
    private String gender;
    private String oldPassword;
    private String password;
    private String confirmPassword;
}
