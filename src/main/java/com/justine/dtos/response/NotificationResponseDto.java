package com.justine.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private String title;
    private String message;
    private String severity;

    private List<GuestResponseDTO> guests;
    private List<StaffResponseDTO> staffs;
}
