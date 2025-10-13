package com.justine.dtos.response;

import com.justine.enums.StaffRole;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private StaffRole role;
    private String gender;
    private String accessToken;
    private String refreshToken;
    private HotelResponseDTO hotel;
}
