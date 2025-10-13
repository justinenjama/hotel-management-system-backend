package com.justine.dtos.request;

import com.justine.enums.FoodCategory;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemRequestDTO {
    private String itemName;
    private Double price;
    private FoodCategory category;
}
