package com.justine.dtos.response;

import com.justine.enums.FoodCategory;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemResponseDTO {
    private Long id;
    private String itemName;
    private Double price;
    private FoodCategory category;
    private String imageUrl;
    private Long hotelId;
}
