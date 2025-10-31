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
    private String hotelImageUrl;
    private List<RoomResponseDTO> rooms;

    /**
     * Optionally include staff, but it's often better to exclude
     * from default responses to avoid large nested payloads.
     */
    private List<StaffResponseDTO> staffMembers;
}
