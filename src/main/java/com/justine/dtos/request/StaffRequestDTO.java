package com.justine.dtos.request;

import com.justine.enums.StaffRole;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRequestDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private String password;
    private String confirmPassword;
    private StaffRole role;
    private Long hotelId;
}
