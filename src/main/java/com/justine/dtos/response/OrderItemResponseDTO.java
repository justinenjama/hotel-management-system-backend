package com.justine.dtos.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private Long id;
    private int quantity;
    private FoodItemResponseDTO foodItem;
}
