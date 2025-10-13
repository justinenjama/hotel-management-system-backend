package com.justine.dtos.request;

import com.justine.enums.RoomType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequestDTO {
    private String roomNumber;
    private RoomType type;
    private Double pricePerNight;
    private boolean available;
    private Long hotelId;
}
