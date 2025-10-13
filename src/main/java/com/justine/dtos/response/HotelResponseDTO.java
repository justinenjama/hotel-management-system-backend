package com.justine.dtos.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponseDTO {
    private Long id;
    private String name;
    private String location;
    private String contactNumber;
    private String email;
    private List<RoomResponseDTO> rooms;
    private List<StaffResponseDTO> staffMembers;
}
