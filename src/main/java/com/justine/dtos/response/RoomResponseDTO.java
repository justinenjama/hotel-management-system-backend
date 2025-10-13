package com.justine.dtos.response;

import com.justine.enums.RoomType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private Long id;
    private String roomNumber;
    private RoomType type;
    private Double pricePerNight;
    private boolean available;
    private HotelResponseDTO hotel;
}
