package com.justine.dtos.request;

import com.justine.enums.FoodCategory;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemRequestDTO {
    private String itemName;
    private Double price;
    private FoodCategory category;
    private String imageUrl;
    private Long hotelId;
}
