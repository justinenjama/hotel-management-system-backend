package com.justine.dtos.response;

import com.justine.enums.FoodCategory;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemResponseDTO {
    private Long id;
    private String itemName;
    private Double price;
    private FoodCategory category;
}
